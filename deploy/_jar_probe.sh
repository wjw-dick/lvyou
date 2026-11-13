#!/bin/bash
echo "== APP_DIR =="
ls -la /home/app/travel.jxdg.net/
echo "== jar file 类型 =="
file /home/app/travel.jxdg.net/travel-platform.jar 2>/dev/null
echo "== 磁盘 =="
df -h /home/app | tail -2
echo "== 直连 java -jar 试跑(前5秒) =="
cd /home/app/travel.jxdg.net && timeout 5 /usr/lib/jvm/jdk-17/bin/java -Xms128m -Xmx256m -jar travel-platform.jar 2>&1 | head -8
echo "== DONE =="
