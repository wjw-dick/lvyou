#!/bin/bash
echo "=== travel 库表数量 ==="
mysql -h127.0.0.1 -P3306 -uroot -p85b5c930502992d4 -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='travel'" 2>/dev/null
echo "=== travel 库表列表(前20) ==="
mysql -h127.0.0.1 -P3306 -uroot -p85b5c930502992d4 -N -e "SELECT table_name FROM information_schema.tables WHERE table_schema='travel' LIMIT 20" 2>/dev/null
