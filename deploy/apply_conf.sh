#!/bin/bash
set -e
CONF=/www/server/panel/vhost/nginx/travel.jxdg.net.conf
echo "=== 备份原 conf ==="
cp "$CONF" "$CONF.bak.$(date +%s)" 2>&1
ls -la "$CONF"*
echo "=== 上传新 conf 到 /tmp 已完成，复制到面板 vhost ==="
cp /tmp/travel.jxdg.net.conf "$CONF"
echo "=== nginx -t 校验 ==="
/www/server/nginx/sbin/nginx -t 2>&1 || echo "NGINX_TEST_FAIL"
echo "DONE"
