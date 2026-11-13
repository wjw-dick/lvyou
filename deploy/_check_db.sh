#!/bin/bash
echo "== travel 表数量 =="
mysql -h127.0.0.1 -uroot -p85b5c930502992d4 -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='travel'" 2>/dev/null
echo "== travel 表列表(前20) =="
mysql -h127.0.0.1 -uroot -p85b5c930502992d4 -N -e "SHOW TABLES FROM travel" 2>/dev/null | head -20
echo "== DONE =="
