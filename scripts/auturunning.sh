#!/bin/bash

if sudo lsof -i :80 > /dev/null; then
  sudo kill -9 $(sudo lsof -t -i :80)
  echo '정상적으로 종료되었습니다.'
fi
sudo chmod +x ./app/demoApp-0.0.1-SNAPSHOT.jar
sudo nohup java -jar ./app/demoApp-0.0.1-SNAPSHOT.jar \
--spring.datasource.url="database_url" \
--spring.datasource.username="username" \
--spring.datasource.password="password" &
echo "배포완료 및 재 실행됩니다."
