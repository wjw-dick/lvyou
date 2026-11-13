#!/bin/bash
echo "== 端口 18086 =="
ss -ltnp 2>/dev/null | grep ':18086 ' || echo NOT_LISTENING
echo "== 进程 =="
ps aux | grep travel-platform.jar | grep -v grep | head -1 || echo NO_PROC
echo "== 健康接口直连 =="
curl -s --max-time 10 -o /dev/null -w 'http=%{http_code}\n' http://127.0.0.1:18086/api/actuator/health 2>&1 || echo FAIL
curl -s --max-time 10 http://127.0.0.1:18086/api/actuator/health 2>&1 | head -c 200
echo ""
echo "== 日志尾部 =="
tail -30 /home/app/travel.jxdg.net/travel.log 2>/dev/null
echo "== DONE =="
