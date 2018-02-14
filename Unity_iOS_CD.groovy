pipeline{
    agent {
        label 'build.machine'
    }

    environment {
        S3_BUCKET = 'application.release'
        GIT_URL = 'git@github.com:rafaftahsin/Unity-Jenkinsfile-Exmaple.git'
    }

    stages{
        stage('Git Clonee'){
            steps {
                slackSend channel: '#hal-alerts', message: "${env.JOB_NAME} is started"
                deleteDir()
                git branch: 'development', credentialsId: 'Jenkins Master SSH', url: "${GIT_URL}"
            }
        }
        
        stage('Build Step'){
            steps {
                sh '''
                    rm -rf Builds/iOS/*
                    cd Assets && nuget restore && cd ..
                    /Applications/Unity/Unity.app/Contents/MacOS/Unity -batchmode -nographics -projectPath "$(pwd)" -logFile unitylog.log -executeMethod AutoBuilder.PerformiOSBuild -quit
                    cp ProjectSettings/exportOptions.plist Builds/iOS/
                    cd 'Builds/iOS/'
                    echo "OTHER_LDFLAGS = \\$(inherited) -framework Speech" > config.xconfig
                    xcodebuild -project Unity-iPhone.xcodeproj -scheme Unity-iPhone -allowProvisioningUpdates -xcconfig config.xconfig -configuration Release build -archivePath 'MyBuddy.xcarchive' archive DEVELOPMENT_TEAM=B48346ETV3
                    xcodebuild -exportArchive -archivePath 'MyBuddy.xcarchive' -exportPath 'MyBuddy' -exportOptionsPlist exportOptions.plist
                '''
            }
        }
        
        stage('S3 Upload'){
            steps {
                sh '''
                    ipa_version=$(cat ProjectSettings/iOS_build_version)
                    ipa_name="ApplicationName-$ipa_version.ipa"
                    
                    if [ -f Builds/iOS/MyBuddy/Unity-iPhone.ipa ]; then
                        s3cmd put -r "Builds/iOS/MyBuddy/Unity-iPhone.ipa" "${S3_BUCKET}/iOS/$ipa_name"
                        
                        mejor_version=$(echo $ipa_version | cut -d'.' -f1)
                        minor_version=$(echo $ipa_version | cut -d'.' -f2)
                        build_version=$(echo $ipa_version | cut -d'.' -f3)

                        next_build_version=$(($build_version + 1))

                        next_ipa_version="$mejor_version.$minor_version.$next_build_version"

                        echo "$next_ipa_version" > ProjectSettings/iOS_build_version
                        
                        git add ProjectSettings/iOS_build_version
                        git commit -m "version updated"
                        git push origin development

                    else
                        echo "IPA is not found"
                        
                    fi
                    
                '''
            }
        }
        stage('Slack Notification'){
            steps {
                script{
                    next_version = sh(script: "cat ProjectSettings/iOS_build_version", returnStdout: true).trim()

                    mejor_version = sh(script: "echo $next_version | cut -d'.' -f1", returnStdout: true).trim()
                    minor_version = sh(script: "echo $next_version | cut -d'.' -f2", returnStdout: true).trim()
                    build_version = sh(script: "echo $next_version | cut -d'.' -f3", returnStdout: true).trim()

                    build_version = sh(script: "echo \$(($build_version - 1))" , returnStdout: true).trim()
                    ipa_version = sh(script: "echo $mejor_version.$minor_version.$build_version", returnStdout: true).trim()
                    
                    // apk_name = sh(script: "echo ApplicationName-$apk_version.apk", returnStdout: true).trim()
                    
                    slackSend channel: '#hal-alerts', message: "${env.JOB_NAME} is completed. Please Download the ipa from https://s3-ap-southeast-1.amazonaws.com/infolytx.mindful-buddy.release/iOS/ApplicationName-${ipa_version}.ipa"
                }
                
            }
        }
        
    }
}