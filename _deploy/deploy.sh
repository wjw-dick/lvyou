#!/bin/bash
set -e
SRC=/tmp/travel
APP_DIR=/home/app/lvyou.quantaida.cloud
W1=/www/wwwroot/lvyou.quantaida.cloud
W2=/www/wwwroot/lvyou-admin.quantaida.cloud

echo "== app dir =="
mkdir -p "$APP_DIR/logs" "$APP_DIR/data/upload" "$APP_DIR/data/backup"
mv -f "$SRC/app.jar" "$APP_DIR/app.jar"
cp -f "$SRC/env" "$APP_DIR/.env"
cp -f "$SRC/service.sh" "$APP_DIR/service.sh"
chmod 755 "$APP_DIR/service.sh"
chmod 600 "$APP_DIR/.env"
ls -lh "$APP_DIR"

echo "== frontend deploy =="
mkdir -p "$W1" "$W2"
rm -rf "$W1"/assets "$W2"/assets "$W1"/index.html "$W2"/index.html
unzip -oq "$SRC/lvyou-main.zip" -d "$W1"
unzip -oq "$SRC/lvyou-admin.zip" -d "$W2"
ls "$W1" | head -5
ls "$W2" | head -5

echo "== cert =="
for d in lvyou.quantaida.cloud lvyou-admin.quantaida.cloud; do
  mkdir -p "/www/server/panel/vhost/cert/$d"
  cp -f /www/server/panel/vhost/cert/710auto.quantaida.cloud/fullchain.pem "/www/server/panel/vhost/cert/$d/fullchain.pem"
  cp -f /www/server/panel/vhost/cert/710auto.quantaida.cloud/privkey.pem "/www/server/panel/vhost/cert/$d/privkey.pem"
  chmod 600 "/www/server/panel/vhost/cert/$d/fullchain.pem" "/www/server/panel/vhost/cert/$d/privkey.pem"
done
openssl x509 -in "/www/server/panel/vhost/cert/lvyou.quantaida.cloud/fullchain.pem" -noout -subject -enddate

echo "== permissions =="
chown -R www:www "$W1" "$W2" 2>/dev/null || true
chmod 755 "$W1" "$W2" 2>/dev/null || true
chown -R root:root "$APP_DIR"

echo "== nginx conf =="
cp -f "$SRC/lvyou.quantaida.cloud.conf" /www/server/panel/vhost/nginx/lvyou.quantaida.cloud.conf
cp -f "$SRC/lvyou-admin.quantaida.cloud.conf" /www/server/panel/vhost/nginx/lvyou-admin.quantaida.cloud.conf
/www/server/nginx/sbin/nginx -t 2>&1 | tail -2
/www/server/nginx/sbin/nginx -s reload && echo RELOAD_OK
