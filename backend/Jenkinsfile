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
                // 백업 디렉토리 준비
                sh "sudo mkdir -p ${DEPLOY_DIR}/backup"

                // 기존 jar 백업 (첫 배포면 스킵)
                sh """
                    if [ -f ${DEPLOY_DIR}/${JAR_NAME} ]; then
                        TS=\$(date +%Y%m%d-%H%M%S)
                        sudo cp ${DEPLOY_DIR}/${JAR_NAME} ${DEPLOY_DIR}/backup/shield-\${TS}.jar
                        echo "Backup created: shield-\${TS}.jar"
                    else
                        echo "No existing jar to backup (first deploy)"
                    fi
                """

                // 최근 5개만 유지 (오래된 백업 삭제)
                sh """
                    cd ${DEPLOY_DIR}/backup
                    sudo ls -t shield-*.jar 2>/dev/null | tail -n +6 | xargs -r sudo rm -v || true
                """

                // 새 jar 배포
                sh "sudo cp build/libs/shield-*.jar ${DEPLOY_DIR}/${JAR_NAME}"
                sh 'sudo systemctl restart shield-backend'
            }
        }

        stage('Health Check') {
            steps {
                sh 'sleep 40'
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
