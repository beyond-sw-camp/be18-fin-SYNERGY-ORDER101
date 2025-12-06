#!/bin/bash
echo "=== ApplicationStart: starting python-server ==="

cd /home/ec2-user/python-server

REGION="ap-northeast-2"
PARAM_PREFIX="/prod/synergy"

echo "Loading DB parameters..."

DB_HOST=$(aws ssm get-parameter --name "${PARAM_PREFIX}/db_host" --with-decryption --region $REGION --query "Parameter.Value" --output text)
DB_PORT=$(aws ssm get-parameter --name "${PARAM_PREFIX}/db_port" --with-decryption --region $REGION --query "Parameter.Value" --output text)
DB_USER=$(aws ssm get-parameter --name "${PARAM_PREFIX}/db_username" --with-decryption --region $REGION --query "Parameter.Value" --output text)
DB_PASSWORD=$(aws ssm get-parameter --name "${PARAM_PREFIX}/db_password" --with-decryption --region $REGION --query "Parameter.Value" --output text)
DB_NAME=$(aws ssm get-parameter --name "${PARAM_PREFIX}/db_name" --with-decryption --region $REGION --query "Parameter.Value" --output text)

echo "Loaded:"
echo "DB_HOST=$DB_HOST"
echo "DB_PORT=$DB_PORT"
echo "DB_NAME=$DB_NAME"

docker stop python-server || true
docker rm python-server || true

docker build -t python-server .

docker run -d \
    --name python-server \
    -p 8000:8000 \
    -e DB_HOST="$DB_HOST" \
    -e DB_PORT="$DB_PORT" \
    -e DB_USER="$DB_USER" \
    -e DB_PASSWORD="$DB_PASSWORD" \
    -e DB_NAME="$DB_NAME" \
    python-server

echo "Python server started!"
