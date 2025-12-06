#!/bin/bash
# CodeDeploy Hook: ApplicationStart
# 새 버전의 JAR 파일을 백그라운드에서 실행합니다.

# appspec.yml의 destination과 동일
export PATH=$PATH:/usr/local/bin:/usr/bin

DEPLOY_PATH="/opt/synergy-backend"
LOG_PATH="/var/log/synergy/backend.log"
# JAR 파일 찾기 (배포 시 target/ 폴더 내용물이 직접 복사됨)
JAR_FILE=$(ls -t $DEPLOY_PATH/*.jar 2>/dev/null | head -n 1)

if [ -z "$JAR_FILE" ]; then
    echo "ERROR: [$DEPLOY_PATH] 경로에서 실행할 JAR 파일을 찾을 수 없습니다."
    exit 1
fi

# 로그 디렉토리 생성 (CodeDeploy는 파일만 복사하므로 디렉토리 생성은 스크립트가 담당)
mkdir -p $(dirname "$LOG_PATH")
echo "Log directory [$LOG_PATH] created."

# ===== 환경 변수 설정 =====
REGION="ap-northeast-2"
PARAM_PREFIX="/prod/synergy"

echo "Loading all environment variables from Parameter Store..."

# ----- 모든 환경 변수를 Parameter Store에서 가져오기 -----
# Parameter Store 이름: /prod/synergy/xxx (소문자, 언더스코어)
export DB_HOST=$(aws ssm get-parameter --name "${PARAM_PREFIX}/db_host" --region $REGION --with-decryption --query "Parameter.Value" --output text)
export DB_PORT=$(aws ssm get-parameter --name "${PARAM_PREFIX}/db_port" --region $REGION --with-decryption --query "Parameter.Value" --output text)
export DB_NAME=$(aws ssm get-parameter --name "${PARAM_PREFIX}/db_name" --region $REGION --with-decryption --query "Parameter.Value" --output text)
export DB_USERNAME=$(aws ssm get-parameter --name "${PARAM_PREFIX}/db_username" --region $REGION --with-decryption --query "Parameter.Value" --output text)
export DB_PASSWORD=$(aws ssm get-parameter --name "${PARAM_PREFIX}/db_password" --region $REGION --with-decryption --query "Parameter.Value" --output text)
export REDIS_HOST=$(aws ssm get-parameter --name "${PARAM_PREFIX}/redis_host" --region $REGION --with-decryption --query "Parameter.Value" --output text)
export REDIS_PORT=$(aws ssm get-parameter --name "${PARAM_PREFIX}/redis_port" --region $REGION --with-decryption --query "Parameter.Value" --output text)
export REDIS_PASSWORD=$(aws ssm get-parameter --name "${PARAM_PREFIX}/redis_password" --region $REGION --with-decryption --query "Parameter.Value" --output text)
export JWT_SECRET=$(aws ssm get-parameter --name "${PARAM_PREFIX}/jwt" --region $REGION --with-decryption --query "Parameter.Value" --output text)
export AI_HOST=$(aws ssm get-parameter --name "${PARAM_PREFIX}/ai_host" --region $REGION --with-decryption --query "Parameter.Value" --output text)
export AI_PORT=$(aws ssm get-parameter --name "${PARAM_PREFIX}/ai_port" --region $REGION --with-decryption --query "Parameter.Value" --output text)
export AWS_ACCESS_KEY=$(aws ssm get-parameter --name "${PARAM_PREFIX}/s3_accsee" --region $REGION --with-decryption --query "Parameter.Value" --output text)
export AWS_SECRET_KEY=$(aws ssm get-parameter --name "${PARAM_PREFIX}/s3_password" --region $REGION --with-decryption --query "Parameter.Value" --output text)

echo "Environment variables loaded successfully."
# ===== 환경 변수 설정 완료 =====

echo "Environment variables loaded successfully."
# ===== 환경 변수 설정 완료 =====

echo "Starting application: $JAR_FILE"

# nohup을 사용하여 백그라운드에서 실행하며, 
# 표준 출력과 에러 출력을 로그 파일로 리다이렉트합니다.
# 주의: --spring.profiles.active=prod 부분은 실제 환경에 맞게 조정하세요.
nohup java -jar $JAR_FILE \
    --spring.profiles.active=prod \
    --server.port=8080 \
    > $LOG_PATH 2>&1 &

echo "Application started successfully. Check log at $LOG_PATH"