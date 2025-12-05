#!/bin/bash
REGION="ap-northeast-2"
PARAM_PREFIX="/prod/synergy"

echo "Loading environment variables from SSM Parameter Store..."

export DB_HOST=$(aws ssm get-parameter --name "${PARAM_PREFIX}/db_host" --with-decryption --region $REGION --query Parameter.Value --output text)
export DB_PORT=$(aws ssm get-parameter --name "${PARAM_PREFIX}/db_port" --with-decryption --region $REGION --query Parameter.Value --output text)
export DB_NAME=$(aws ssm get-parameter --name "${PARAM_PREFIX}/db_name" --with-decryption --region $REGION --query Parameter.Value --output text)
export DB_USERNAME=$(aws ssm get-parameter --name "${PARAM_PREFIX}/db_username" --with-decryption --region $REGION --query Parameter.Value --output text)
export DB_PASSWORD=$(aws ssm get-parameter --name "${PARAM_PREFIX}/db_password" --with-decryption --region $REGION --query Parameter.Value --output text)

export AI_HOST=$(aws ssm get-parameter --name "${PARAM_PREFIX}/ai_host" --region $REGION --query Parameter.Value --output text)
export AI_PORT=$(aws ssm get-parameter --name "${PARAM_PREFIX}/ai_port" --region $REGION --query Parameter.Value --output text)

export REDIS_HOST=$(aws ssm get-parameter --name "${PARAM_PREFIX}/redis_host" --region $REGION --query Parameter.Value --output text)
export REDIS_PORT=$(aws ssm get-parameter --name "${PARAM_PREFIX}/redis_port" --region $REGION --query Parameter.Value --output text)
export REDIS_PASSWORD=$(aws ssm get-parameter --name "${PARAM_PREFIX}/redis_password" --region $REGION --query Parameter.Value --output text)

echo "Starting Python container..."

docker run -d \
  --name order101-ai \
  -p 8000:8000 \
  -e DB_HOST=$DB_HOST \
  -e DB_PORT=$DB_PORT \
  -e DB_NAME=$DB_NAME \
  -e DB_USER=$DB_USERNAME \
  -e DB_PASSWORD=$DB_PASSWORD \
  order101-ai
