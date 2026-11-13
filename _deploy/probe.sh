#!/bin/bash
for p in 18083 18084 18085; do
  if ss -ltn | grep -q ":$p "; then echo "$p BUSY"; else echo "$p FREE"; fi
done
echo "== db travel =="
mysql -uroot -pbr13jhhrh6 -N -e "SHOW DATABASES LIKE 'travel';" 2>/dev/null || echo "mysql-auth-fail"
echo "== sites like lvyou =="
sqlite3 /www/server/panel/data/db/site.db "SELECT id,name,path FROM sites WHERE name LIKE '%lvyou%';" 2>/dev/null || echo "sqlite-fail"
echo "== jdk =="
ls /www/server/java/ 2>/dev/null
