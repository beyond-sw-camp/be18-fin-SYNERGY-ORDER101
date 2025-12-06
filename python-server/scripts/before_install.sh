#!/bin/bash
echo "BeforeInstall: cleaning old containers"

docker stop python-server || true
docker rm python-server || true

mkdir -p /home/ec2-user/python-server
