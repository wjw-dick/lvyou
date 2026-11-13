#!/bin/bash
VHOST=/www/server/panel/vhost/nginx
cp -a "$VHOST/travel.jxdg.net.conf" "$VHOST/travel.jxdg.net.conf.panel.bak" 2>&1
cp -a "$VHOST/travel-admin.jxdg.net.conf" "$VHOST/travel-admin.jxdg.net.conf.panel.bak" 2>&1
cp -f /tmp/conf-main.conf "$VHOST/travel.jxdg.net.conf"
cp -f /tmp/conf-admin.conf "$VHOST/travel-admin.jxdg.net.conf"
echo "== nginx -t =="
/www/server/nginx/sbin/nginx -t 2>&1
echo "RC_T=$?"
echo "== reload =="
/www/server/nginx/sbin/nginx -s reload 2>&1
echo "RC_R=$?"
echo "== conf 列表 =="
ls -la "$VHOST" | grep travel
echo "== DONE =="
