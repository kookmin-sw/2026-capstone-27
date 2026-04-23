#!/bin/bash
# SHIELD 백엔드 롤백 스크립트
# 사용법: sudo bash /home/ec2-user/shield/rollback.sh

set -e

DEPLOY_DIR=/home/ec2-user/shield
BACKUP_DIR=${DEPLOY_DIR}/backup
JAR=${DEPLOY_DIR}/shield.jar

if [ ! -d "${BACKUP_DIR}" ] || [ -z "$(ls -A ${BACKUP_DIR} 2>/dev/null)" ]; then
    echo "❌ 백업 파일이 없습니다: ${BACKUP_DIR}"
    echo "   최소 1회 이상 배포가 진행된 후에만 롤백 가능합니다."
    exit 1
fi

echo "=========================================="
echo "  SHIELD 롤백 스크립트"
echo "=========================================="
echo ""
echo "사용 가능한 백업 목록 (최신순):"
echo ""
ls -lt ${BACKUP_DIR}/shield-*.jar | awk '{print "  "$9"  ("$6" "$7" "$8")"}'
echo ""
read -p "복원할 파일명 (예: shield-20260423-140000.jar): " TARGET

if [ -z "${TARGET}" ]; then
    echo "❌ 파일명이 비어있습니다"
    exit 1
fi

if [ ! -f "${BACKUP_DIR}/${TARGET}" ]; then
    echo "❌ 파일을 찾을 수 없음: ${BACKUP_DIR}/${TARGET}"
    exit 1
fi

echo ""
echo "롤백 준비:"
echo "  - 대상: ${BACKUP_DIR}/${TARGET}"
echo "  - 적용: ${JAR}"
echo ""
read -p "진행하시겠습니까? (y/N): " CONFIRM

if [ "${CONFIRM}" != "y" ] && [ "${CONFIRM}" != "Y" ]; then
    echo "취소되었습니다"
    exit 0
fi

# 현재 운영 jar도 한 번 더 백업 (롤백의 롤백 대비)
CURRENT_TS=$(date +%Y%m%d-%H%M%S)
sudo cp "${JAR}" "${BACKUP_DIR}/shield-${CURRENT_TS}-before-rollback.jar"
echo "✓ 현재 jar를 ${BACKUP_DIR}/shield-${CURRENT_TS}-before-rollback.jar 로 백업"

# 롤백 수행
sudo cp "${BACKUP_DIR}/${TARGET}" "${JAR}"
sudo systemctl restart shield-backend
echo "✓ 서비스 재시작 중..."

sleep 15

# 헬스체크
if curl -sf http://localhost:8080/actuator/health > /dev/null; then
    echo ""
    echo "✅ 롤백 성공: ${TARGET}"
    sudo systemctl status shield-backend --no-pager | head -3
else
    echo ""
    echo "❌ 헬스체크 실패 - 로그 확인 필요"
    echo "   sudo journalctl -u shield-backend -n 50 --no-pager"
    exit 1
fi
