pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    parameters {
        booleanParam(name: 'DEPLOY', defaultValue: true, description: 'Deploy the jar after a green build')
        string(name: 'DEPLOY_DIR', defaultValue: '', description: 'Target folder. Empty = <workspace>/../clinicbook-deploy')
        string(name: 'APP_PORT', defaultValue: '8081', description: 'Port of the deployed application')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                script {
                    if (isUnix()) {
                        sh 'chmod +x mvnw && ./mvnw -B clean verify'
                    } else {
                        bat 'mvnw.cmd -B clean verify'
                    }
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true, onlyIfSuccessful: true
            }
        }

        stage('Deploy') {
            when {
                expression { return params.DEPLOY }
            }
            steps {
                script {
                    def dir = params.DEPLOY_DIR?.trim()
                    if (isUnix()) {
                        dir = dir ?: "${env.WORKSPACE}/../clinicbook-deploy"
                        sh "chmod +x deploy/deploy.sh && BUILD_ID=dontKillMe JENKINS_NODE_COOKIE=dontKillMe ./deploy/deploy.sh \"\$(ls target/clinicbook-*.jar | grep -v original | head -n 1)\" \"${dir}\" ${params.APP_PORT}"
                    } else {
                        dir = dir ?: "${env.WORKSPACE}\\..\\clinicbook-deploy"
                        bat "set JENKINS_NODE_COOKIE=dontKillMe&& deploy\\deploy.bat \"%CD%\\target\\clinicbook-0.0.1-SNAPSHOT.jar\" \"${dir}\" ${params.APP_PORT}"
                    }
                }
            }
        }
    }

    post {
        success {
            echo "Build OK. App: http://localhost:${params.APP_PORT}/ (if deployed)"
        }
        failure {
            echo 'Build FAILED - open the Console Output / Test Result of this build.'
        }
    }
}
