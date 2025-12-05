#!/bin/bash
# CodeDeploy Hook: AfterInstall
# 복사된 JAR 파일과 스크립트 파일에 실행 권한을 부여합니다.

# appspec.yml의 destination과 동일
DEPLOY_PATH="/opt/synergy-backend"

# 복사된 JAR 파일 이름 찾기
JAR_FILE=$(ls $DEPLOY_PATH | grep .jar | head -n 1)

echo "Changing execution permissions for $JAR_FILE"
# JAR 파일에 실행 권한 부여
chmod +x $DEPLOY_PATH/$JAR_FILE

# 스크립트 파일에 실행 권한 부여 (혹시 모를 경우를 대비한 안전 장치)
chmod +x $DEPLOY_PATH/scripts/*.sh

echo "Permissions updated successfully."