#!/bin/bash
echo "BeforeInstall: cleaning old containers"

# docker daemon 확인
if ! docker info >/dev/null 2>&1; then
    echo "Docker daemon is not running. Starting docker..."
    systemctl start docker
fi

# 컨테이너 정리
docker stop python-server >/dev/null 2>&1 || true
docker rm python-server >/dev/null 2>&1 || true

# 디렉토리 생성
mkdir -p /home/ec2-user/python-server || true

echo "BeforeInstall: completed successfully"
