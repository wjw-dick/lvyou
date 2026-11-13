#!/bin/bash
BASE=/home/www/wwwroot/travel.jxdg.net
APP=/home/app/travel.jxdg.net
echo "== /tmp dist 副本 =="
ls /tmp/travel-portal-dist 2>&1 | head -3
ls /tmp/travel-admin-dist 2>&1 | head -3
echo "== 重新落位 portal =="
rm -f "$BASE/index.html" "$BASE/404.html" "$BASE/.htaccess" "$BASE/.user.ini"
cp -r /tmp/travel-portal-dist/. "$BASE/"
echo "== 重新落位 admin =="
find "$APP/admin" -type f -delete 2>/dev/null
cp -r /tmp/travel-admin-dist/. "$APP/admin/"
chown -R www:www "$BASE" "$APP"
chmod -R 755 "$BASE" "$APP"
echo "== 校验 portal =="
ls "$BASE" | tr '\n' ' '; echo
head -c 200 "$BASE/index.html"; echo
echo "== 校验 admin =="
ls "$APP/admin" | tr '\n' ' '; echo
head -c 200 "$APP/admin/index.html"; echo
echo "== 权限 =="
stat -c '%n %U:%G %a' "$BASE" "$APP/admin"
echo "== DONE =="
