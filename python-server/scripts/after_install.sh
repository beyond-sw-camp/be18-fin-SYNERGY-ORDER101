#!/bin/bash
echo "AfterInstall: preparing..."

cd /home/ec2-user/python-server

# 디렉토리 권한 수정 (여기 추가)
chown -R ec2-user:ec2-user /home/ec2-user/python-server
chmod -R 755 /home/ec2-user/python-server

chmod -R 755 scripts
