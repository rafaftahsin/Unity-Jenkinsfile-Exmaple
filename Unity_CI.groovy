pipeline{
    agent {
        label 'build.machine'
    }

    environment {
        GIT_URL = ''
    }

    
    stages{
        stage('Git Clonee'){
            steps {
                deleteDir()
                slackSend channel: '#hal-alerts', message: "${env.JOB_NAME} is started"
                git branch: 'development', credentialsId: 'Jenkins Master SSH', url: "${GIT_URL}"
            }
        }

        stage('Build'){
            steps {
                sh '''
                   /Applications/Unity/Unity.app/Contents/MacOS/Unity -batchmode -nographics -projectPath "$(pwd)" -logFile unitylog.log -quit
                '''
            }
        }
        stage('Run Test'){
            steps {
                script {
                    try {
                        sh '''
                           /Applications/Unity/Unity.app/Contents/MacOS/Unity -batchmode -runEditorTests "$(pwd)" -testFilter "$(pwd)/Library/ScriptAssemblies/Assembly-CSharp.dll" -editorTestsResultFile "$(pwd)/unit_test_result.xml"
                        '''
                    }
                    catch (exc) {
                        script{
                            sh 'cat unit_test_result.xml'
                            error("Build Failed Due to Test Fail.")
                        }
                    }
                }
            }
        }
    }

    post { 
        success { 
            script{
                slackSend channel: '#hal-alerts', message: "${env.JOB_NAME} is completed SUCCESSFULLY."
            }
        }

        failure { 
            script{
                slackSend channel: '#hal-alerts', message: "${env.JOB_NAME} is FAILED. See ${env.BUILD_URL} console for details."
            }
        }
    }
}
