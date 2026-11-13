#!/bin/bash
echo "=== travel 数据库 ==="
mysql -h127.0.0.1 -P3306 -uroot -p85b5c930502992d4 -N -e "SHOW DATABASES LIKE 'travel'" 2>&1
echo "=== travel 应用账号 ==="
mysql -h127.0.0.1 -P3306 -uroot -p85b5c930502992d4 -N -e "SELECT user,host FROM mysql.user WHERE user='travel'" 2>&1
echo "=== wwwroot 已有站点目录 ==="
ls /home/www/wwwroot/ 2>&1
echo "=== /home/app 已有应用 ==="
ls /home/app/ 2>&1
echo "=== 全部监听端口(简短) ==="
netstat -tln 2>/dev/null | awk '{print $4}' | grep -oE ':[0-9]+' | sort -u | tr '\n' ' '
echo ""
echo "=== nginx 是否运行 ==="
ps aux | grep -E 'nginx' | grep -v grep | head -1
echo "=== 本机java 版本探测 ==="
ls /usr/lib/jvm/
