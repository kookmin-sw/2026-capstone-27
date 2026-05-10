#!/bin/bash
# SHIELD Backend 배포 스크립트
# 사용: ./deploy/deploy.sh

set -e

EC2_HOST="ec2-user@13.125.245.5"
EC2_KEY="$HOME/.ssh/shield-key.pem"
REMOTE_DIR="/home/ec2-user/shield"
JAR_NAME="shield.jar"

echo "[1/4] Gradle 빌드 중..."
./gradlew clean bootJar -x test

BUILD_JAR=$(find build/libs -name "*.jar" ! -name "*-plain.jar" | head -1)
if [ -z "$BUILD_JAR" ]; then
    echo "빌드 실패: jar 파일을 찾을 수 없음"
    exit 1
fi
echo "빌드 완료: $BUILD_JAR"

echo "[2/4] EC2로 jar 전송 중..."
scp -i "$EC2_KEY" "$BUILD_JAR" "$EC2_HOST:$REMOTE_DIR/$JAR_NAME"

echo "[3/4] 서비스 재시작 중..."
ssh -i "$EC2_KEY" "$EC2_HOST" "sudo systemctl restart shield-backend"

echo "[4/4] 헬스체크 중..."
sleep 15
HEALTH=$(curl -s -o /dev/null -w "%{http_code}" https://shieldai.kr/actuator/health || echo "000")
if [ "$HEALTH" = "200" ]; then
    echo "배포 성공! HTTP $HEALTH"
else
    echo "배포 확인 필요: HTTP $HEALTH"
    echo "로그 확인: ssh -i $EC2_KEY $EC2_HOST 'sudo journalctl -u shield-backend -n 50'"
fi
