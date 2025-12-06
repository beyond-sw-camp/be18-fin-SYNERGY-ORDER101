#!/bin/bash
# CodeDeploy Hook: ValidateService
# 배포 후 Spring Boot Actuator health check로 서비스 정상 동작 확인

PROJECT_NAME="order101"
HEALTH_URL="http://localhost:8080/actuator/health"
MAX_RETRIES=10
SLEEP_SEC=5

for i in $(seq 1 $MAX_RETRIES); do
    STATUS=$(curl -s $HEALTH_URL | grep 'UP')
    if [ -n "$STATUS" ]; then
        echo "[$PROJECT_NAME] Health check 성공: 서비스가 정상적으로 동작 중입니다. ($HEALTH_URL)"
        exit 0
    fi
    echo "[$PROJECT_NAME] Health check 실패: 재시도 중 ($i/$MAX_RETRIES)"
    sleep $SLEEP_SEC
    # 프로세스가 아예 안 떠 있으면 빠르게 실패 처리
    PID=$(ps -ef | grep java | grep "$PROJECT_NAME" | grep ".jar" | grep -v grep | awk '{print $2}')
    if [ -z "$PID" ]; then
        echo "[$PROJECT_NAME] 백엔드 프로세스가 실행 중이지 않습니다."
        exit 1
    fi
    # JAR 로그에 에러가 있는지 간단 체크 (선택)
    # tail -n 20 /var/log/synergy/backend.log | grep -i 'error'
    # [ "$?" -eq 0 ] && echo "로그에 에러 감지됨" && exit 1
    # (필요시 주석 해제)
done

echo "[$PROJECT_NAME] Health check 최종 실패: 서비스가 정상적으로 동작하지 않습니다. ($HEALTH_URL)"
exit 1
