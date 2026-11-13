#!/bin/bash
set -e
ROOTPW='br13jhhrh6'
APPUSER='travel'
APPPW='xRfJYJpzTZz8JEJj'

mysql -uroot -p"$ROOTPW" -e "DROP DATABASE IF EXISTS travel;" 2>/dev/null || true
mysql -uroot -p"$ROOTPW" --default-character-set=utf8mb4 < /tmp/travel/sql/schema.sql
mysql -uroot -p"$ROOTPW" --default-character-set=utf8mb4 < /tmp/travel/sql/data.sql
mysql -uroot -p"$ROOTPW" -e "
CREATE USER IF NOT EXISTS '$APPUSER'@'%' IDENTIFIED BY '$APPPW';
CREATE USER IF NOT EXISTS '$APPUSER'@'localhost' IDENTIFIED BY '$APPPW';
ALTER USER '$APPUSER'@'%' IDENTIFIED BY '$APPPW';
ALTER USER '$APPUSER'@'localhost' IDENTIFIED BY '$APPPW';
GRANT ALL PRIVILEGES ON travel.* TO '$APPUSER'@'%';
GRANT ALL PRIVILEGES ON travel.* TO '$APPUSER'@'localhost';
FLUSH PRIVILEGES;" 2>/dev/null

echo "== tables =="
mysql -uroot -p"$ROOTPW" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='travel';" 2>/dev/null
echo "== charset =="
mysql -uroot -p"$ROOTPW" -N -e "SELECT default_character_set_name, default_collation_name FROM information_schema.schemata WHERE schema_name='travel';" 2>/dev/null
echo "== sample rows =="
mysql -uroot -p"$ROOTPW" -N -e "SELECT id,tenant_code,tenant_name,status FROM travel.sys_tenant;" 2>/dev/null
echo "== app user connect =="
mysql -u"$APPUSER" -p"$APPPW" -h127.0.0.1 -N -e "SELECT COUNT(*) FROM travel.sys_tenant;" 2>/dev/null && echo APPUSER_OK || echo APPUSER_FAIL
