pipeline {
    agent any

    environment {
        JAVA_HOME = '/usr/lib/jvm/java-21-amazon-corretto.x86_64'
        PATH = "${JAVA_HOME}/bin:${env.PATH}"
        JAR_NAME = 'shield.jar'
        DEPLOY_DIR = '/home/ec2-user/shield'
    }

    stages {
        stage('Build') {
            steps {
                sh 'chmod +x gradlew'
                sh 'java -version'
                sh './gradlew clean bootJar -x test -Dorg.gradle.java.home=${JAVA_HOME}'
            }
        }

        stage('Deploy') {
            steps {
                sh "sudo cp build/libs/shield-*.jar ${DEPLOY_DIR}/${JAR_NAME}"
                sh 'sudo systemctl restart shield-backend'
            }
        }

        stage('Health Check') {
            steps {
                sh 'sleep 20'
                sh 'curl -f http://localhost:8080/actuator/health || exit 1'
            }
        }
    }

    post {
        success {
            echo 'Deploy successful!'
        }
        failure {
            echo 'Deploy failed!'
        }
    }
}
