# #!/bin/bash
# # CodeDeploy Hook: ApplicationStop
# # 현재 실행 중인 Java 프로세스를 안전하게 종료합니다.

# # 프로젝트의 키워드를 기반으로 PID를 찾습니다.
# PROJECT_NAME="order101"
# # 1. JAR 파일명을 포함하는 java 프로세스 찾기
# CURRENT_PID=$(ps -ef | grep java | grep "$PROJECT_NAME" | grep "\.jar" | grep -v grep | awk '{print $2}')

# # 2. 만약 찾지 못하면, 포트 8080을 사용하는 프로세스 찾기 (fallback)
# if [ -z "$CURRENT_PID" ]; then
#     CURRENT_PID=$(lsof -ti:8080 2>/dev/null)
# fi

# if [ -z "$CURRENT_PID" ]; then
#     echo "[$PROJECT_NAME] 실행 중인 애플리케이션 프로세스가 없습니다."
# else
#     echo "[$PROJECT_NAME] PID: $CURRENT_PID, 프로세스를 종료합니다."
#     # 1. SIGTERM (kill -15)으로 프로세스에 종료를 요청합니다.
#     kill -15 $CURRENT_PID
#     sleep 10 # 종료될 때까지 10초 대기
    
#     # 2. 10초 후에도 살아있다면 SIGKILL (kill -9)로 강제 종료합니다.
#     KILL_CHECK=$(ps -p $CURRENT_PID 2>/dev/null | grep -v PID | awk '{print $1}')
#     if [ ! -z "$KILL_CHECK" ]; then
#         echo "[$PROJECT_NAME] 10초 후에도 남아있어 강제 종료(kill -9)합니다."
#         kill -9 $CURRENT_PID
#     fi
#     echo "[$PROJECT_NAME] 애플리케이션 중지 완료."
# fi