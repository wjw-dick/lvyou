#!/bin/bash
echo "== 门户目录现状 =="
ls -la /home/www/wwwroot/travel.jxdg.net/ 2>&1 | head -8
echo "== 上传前 /tmp jar =="
ls -lh /tmp/travel-platform.jar 2>&1
echo "== 落位 =="
mv -f /tmp/travel-platform.jar /home/app/travel.jxdg.net/travel-platform.jar
chown www:www /home/app/travel.jxdg.net/travel-platform.jar
chmod 755 /home/app/travel.jxdg.net/travel-platform.jar
chown www:www /home/app/travel.jxdg.net
ls -la /home/app/travel.jxdg.net/
echo "== 启动 =="
bash /home/app/travel.jxdg.net/start_travel.sh
echo "== 等待40s =="
sleep 40
echo "== 端口 =="
ss -ltnp 2>/dev/null | grep ':18086 ' || echo NOT_LISTENING
echo "== 健康 =="
curl -s --max-time 10 http://127.0.0.1:18086/api/actuator/health 2>&1 | head -c 300
echo ""
echo "== 日志尾部 =="
tail -25 /home/app/travel.jxdg.net/travel.log 2>/dev/null
echo "== DONE =="
