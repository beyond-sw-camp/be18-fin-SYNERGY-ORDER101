#!/bin/bash
# CodeDeploy Hook: ApplicationStart
# 새 버전의 JAR 파일을 백그라운드에서 실행합니다.

# appspec.yml의 destination과 동일
DEPLOY_PATH="/opt/synergy-backend"
LOG_PATH="/var/log/synergy/backend.log"
# target/ 폴더 안에서 JAR 파일 찾기
JAR_FILE=$(ls $DEPLOY_PATH/target/*.jar 2>/dev/null | head -n 1)

# 로그 디렉토리 생성 (CodeDeploy는 파일만 복사하므로 디렉토리 생성은 스크립트가 담당)
mkdir -p $(dirname "$LOG_PATH")
echo "Log directory [$LOG_PATH] created."

echo "Starting application: $JAR_FILE"

# nohup을 사용하여 백그라운드에서 실행하며, 
# 표준 출력과 에러 출력을 로그 파일로 리다이렉트합니다.
# 주의: --spring.profiles.active=prod 부분은 실제 환경에 맞게 조정하세요.
nohup java -jar $JAR_FILE \
    --spring.profiles.active=prod \
    --server.port=8080 \
    > $LOG_PATH 2>&1 &

echo "Application started successfully. Check log at $LOG_PATH"