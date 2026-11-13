#!/bin/bash
echo "== admin_path.pl =="
cat /www/server/panel/data/admin_path.pl 2>/dev/null | od -c | head -5
echo "== default.pl api_sk =="
grep -o -E 'api_sk[^,}]*' /www/server/panel/data/default.pl 2>/dev/null | head -3
echo "== default.pl 全文(前50行) =="
head -50 /www/server/panel/data/default.pl 2>/dev/null
echo "== done =="
