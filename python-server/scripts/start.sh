#!/bin/bash
echo "=== ApplicationStart: starting python-server with secrets ==="

cd /home/ec2-user/python-server

# 1) AWS CLI로 Parameter Store에서 시크릿 로드
DB_HOST=$(aws ssm get-parameter --name "/order101/ai/DB_HOST" --with-decryption --query "Parameter.Value" --output text)
DB_USER=$(aws ssm get-parameter --name "/order101/ai/DB_USER" --with-decryption --query "Parameter.Value" --output text)
DB_PASSWORD=$(aws ssm get-parameter --name "/order101/ai/DB_PASSWORD" --with-decryption --query "Parameter.Value" --output text)

REDIS_HOST=$(aws ssm get-parameter --name "/order101/ai/REDIS_HOST" --with-decryption --query "Parameter.Value" --output text)

JWT_SECRET=$(aws ssm get-parameter --name "/order101/ai/JWT_SECRET" --with-decryption --query "Parameter.Value" --output text)


echo "Loaded parameters:"
echo "DB_HOST=$DB_HOST"
echo "REDIS_HOST=$REDIS_HOST"


# 2) 기존 컨테이너 정리
docker stop python-server || true
docker rm python-server || true

# 3) Docker 이미지 빌드 (EC2 빌드 방식일 경우)
docker build -t python-server .

# 4) Docker run + 환경변수 주입
docker run -d \
    --name python-server \
    -p 8000:8000 \
    -e DB_HOST="$DB_HOST" \
    -e DB_USER="$DB_USER" \
    -e DB_PASSWORD="$DB_PASSWORD" \
    -e REDIS_HOST="$REDIS_HOST" \
    -e JWT_SECRET="$JWT_SECRET" \
    python-server

echo "ApplicationStart completed."
