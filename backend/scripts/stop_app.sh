#!/bin/bash
# CodeDeploy Hook: ApplicationStop
# 현재 실행 중인 Java 프로세스를 안전하게 종료합니다.

PROJECT_NAME="order101"

# 1) 우선 java 프로세스에서 JAR 파일명을 포함하는 PID를 찾습니다.
CURRENT_PID=$(ps -ef | grep java | grep "$PROJECT_NAME" | grep "\.jar" | grep -v grep | awk '{print $2}')

# 2) 찾지 못하면 lsof로 포트(8080) 사용하는 프로세스로 fallback
if [ -z "$CURRENT_PID" ]; then
	if command -v lsof >/dev/null 2>&1; then
		CURRENT_PID=$(lsof -ti:8080 2>/dev/null)
	else
		# lsof가 없으면 netstat + awk 방식으로 확인(대체)
		CURRENT_PID=$(netstat -tulpn 2>/dev/null | grep ':8080' | awk '{print $7}' | cut -d'/' -f1 | head -n1)
	fi
fi

if [ -z "$CURRENT_PID" ]; then
	echo "[$PROJECT_NAME] 실행 중인 애플리케이션 프로세스가 없습니다."
	exit 0
fi

echo "[$PROJEㅁT_NAME] PID: $CURRENT_PID — 중지 시도"

# 1) SIGTERM으로 우선 정상 종료 요청
kill -15 "$CURRENT_PID" 2>/dev/null || true
sleep 10

# 2) 프로세스가 아직 살아 있으면 강제 종료
if ps -p "$CURRENT_PID" > /dev/null 2>&1; then
	echo "[$PROJECT_NAME] SIGTERM으로 종료되지 않아 SIGKILL로 강제 종료합니다."
	kill -9 "$CURRENT_PID" 2>/dev/null || true
	sleep 2
fi

if ps -p "$CURRENT_PID" > /dev/null 2>&1; then
	echo "[$PROJECT_NAME] 프로세스가 여전히 남아있습니다 (PID=$CURRENT_PID). 수동 확인 필요"
	exit 1
else
	echo "[$PROJECT_NAME] 애플리케이션 중지 완료 (PID=$CURRENT_PID)."
fi

exit 0