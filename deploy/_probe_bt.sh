#!/bin/bash
echo "== admin_path.pl =="
cat /www/server/panel/data/admin_path.pl 2>/dev/null
echo "== bt default =="
/etc/init.d/bt default 2>/dev/null
echo "== done =="
