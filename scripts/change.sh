#!/bin/bash

# 설정 변수
WATCH_DIR="/home/ubuntu/app"    # 감시할 디렉토리
SH_FILE="/home/ubuntu/autorunning.sh"
COOLDOWN=10
LAST_RUN=0

# 디렉토리 감지 시작
echo "$(date): $WATCH_DIR 디렉토리 감지를 시작합니다."

# inotifywait로 디렉토리 내 파일 생성, 수정, 삭제, 이동 이벤트 감지
inotifywait -m -r -e create,close_write,delete,moved_to "$WATCH_DIR" |
while read -r _ events filename; do
    CURRENT_TIME=$(date +%s)
    
    # 지정한 쿨다운 시간이 경과했는지 확인
    if (( CURRENT_TIME - LAST_RUN >= COOLDOWN )); then
        echo "$(date): $filename 에서 이벤트 발생: $events. autorunning.sh 실행 중..."
        
        # .sh 파일 실행
        bash "$SH_FILE"
        echo "$(date): autorunning.sh 실행 완료."

        # 마지막 실행 시간 갱신
        LAST_RUN=$CURRENT_TIME
    else
        echo "$(date): 쿨다운 중입니다. 아직 실행하지 않습니다."
    fi
done
