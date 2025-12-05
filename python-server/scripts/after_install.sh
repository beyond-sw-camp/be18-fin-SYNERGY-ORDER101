#!/bin/bash

echo "Logging into ECR..."

aws ecr get-login-password --region ap-northeast-2 \
    | docker login --username AWS --password-stdin 084375551971.dkr.ecr.ap-northeast-2.amazonaws.com

echo "Pulling latest image..."
docker pull 084375551971.dkr.ecr.ap-northeast-2.amazonaws.com/order101-ai:latest
