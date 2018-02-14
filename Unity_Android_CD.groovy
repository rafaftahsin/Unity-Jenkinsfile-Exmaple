pipeline{
    agent {
        label 'build.machine'
    }

    environment {
        S3_BUCKET = 's3.bucket.name'
        GIT_URL = 'git@github.com:rafaftahsin/Unity-Jenkinsfile-Exmaple.git'
    }

    stages{
        stage('Git Clone'){
            steps {
                deleteDir()
                slackSend channel: '#chaneel-name', message: "${env.JOB_NAME} is started"
                git branch: 'development', credentialsId: 'Jenkins Master SSH', url: "${GIT_URL}"
            }
        }
        
        stage('Build Step'){
            steps {
                sh '''
                    rm -rf Builds/android/*
                    cd Assets && nuget restore && cd ..
                    /Applications/Unity/Unity.app/Contents/MacOS/Unity -batchmode -nographics -projectPath "$(pwd)" -logFile unitylog.log -executeMethod AutoBuilder.PerformAndroidBuild -quit
                '''
            }
        }
        
        stage('S3 Upload'){
            steps {
                sh '''
                    apk_version=$(cat ProjectSettings/build_config.json | jq '.BuildVersion' | sed 's/\"//g')
                    apk_name="ApplicationName-$apk_version.apk"
                    
                    if [ -f Builds/android/"$apk_name" ]; then
                        s3cmd put -r "Builds/android/$apk_name" "${S3_BUCKET}/Android/$apk_name"

                        mejor_version=$(echo $apk_version | cut -d'.' -f1)
                        minor_version=$(echo $apk_version | cut -d'.' -f2)
                        build_version=$(echo $apk_version | cut -d'.' -f3)

                        next_build_version=$(($build_version + 1))

                        next_apk_version="$mejor_version.$minor_version.$next_build_version"

                        echo "{ \\"BuildVersion\\" : \\"$next_apk_version\\" }" > ProjectSettings/build_config.json
                        
                        git add ProjectSettings/build_config.json
                        git commit -m "version updated"
                        git push origin development

                    else
                        echo "APK NOT FOUND"
                        
                    fi
                '''
            }
        }


        stage('Slack Notification'){
            steps {
                script{
                    next_version = sh(script: "cat ProjectSettings/build_config.json | jq '.BuildVersion' | sed 's/\"//g'", returnStdout: true).trim()

                    mejor_version = sh(script: "echo $next_version | cut -d'.' -f1", returnStdout: true).trim()
                    minor_version = sh(script: "echo $next_version | cut -d'.' -f2", returnStdout: true).trim()
                    build_version = sh(script: "echo $next_version | cut -d'.' -f3", returnStdout: true).trim()

                    build_version = sh(script: "echo \$(($build_version - 1))" , returnStdout: true).trim()
                    apk_version = sh(script: "echo $mejor_version.$minor_version.$build_version", returnStdout: true).trim()
                    
                    // apk_name = sh(script: "echo ApplicationName-$apk_version.apk", returnStdout: true).trim()
                    
                    slackSend channel: '#channel-name', message: "${env.JOB_NAME} is completed. Please Download the apk from https://s3-ap-southeast-1.amazonaws.com/${S3_BUCKET}/Android/ApplicationName-${apk_version}.apk"
                }
            }
        }
    }
}