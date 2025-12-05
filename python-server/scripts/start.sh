#!/bin/bash

REGION="ap-northeast-2"
PREFIX="/prod/synergy"

echo "Loading parameters from SSM..."

DB_HOST=$(aws ssm get-parameter --name "$PREFIX/db_host" --with-decryption --region $REGION --query "Parameter.Value" --output text)
DB_PORT=$(aws ssm get-parameter --name "$PREFIX/db_port" --region $REGION --query "Parameter.Value" --output text)
DB_NAME=$(aws ssm get-parameter --name "$PREFIX/db_name" --region $REGION --query "Parameter.Value" --output text)
DB_USER=$(aws ssm get-parameter --name "$PREFIX/db_username" --region $REGION --query "Parameter.Value" --output text)
DB_PASSWORD=$(aws ssm get-parameter --name "$PREFIX/db_password" --with-decryption --region $REGION --query "Parameter.Value" --output text)

echo "Starting python-server container..."

docker run -d \
  --name order101-ai \
  -p 8000:8000 \
  -e DB_HOST=$DB_HOST \
  -e DB_PORT=$DB_PORT \
  -e DB_NAME=$DB_NAME \
  -e DB_USER=$DB_USER \
  -e DB_PASSWORD=$DB_PASSWORD \
  084375551971.dkr.ecr.ap-northeast-2.amazonaws.com/order101-ai:latest
