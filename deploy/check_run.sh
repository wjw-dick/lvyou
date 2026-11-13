#!/bin/bash
{
echo "=== 目录 ==="
ls -la /home/app/travel.jxdg.net/
echo "=== PID文件 ==="
cat /home/app/travel.jxdg.net/app.pid 2>&1
echo ""
echo "=== 进程 ==="
ps -ef | grep '[t]ravel-platform'
echo "=== 端口18084 ==="
netstat -tlnp 2>/dev/null | grep ':18084'
echo "=== 日志尾部 ==="
tail -40 /home/app/travel.jxdg.net/travel.log 2>&1
echo "=== CHECK_DONE ==="
} > /tmp/check_travel_run.log 2>&1
