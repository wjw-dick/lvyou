#!/bin/bash
echo "=== 面板 sites 表中 travel ==="
/www/server/panel/pyenv/bin/python <<'EOF'
import sqlite3
conn = sqlite3.connect('/www/server/panel/data/db/site.db')
cols = [c[1] for c in conn.execute("PRAGMA table_info(sites)").fetchall()]
print("sites 列:", cols)
rows = conn.execute("SELECT * FROM sites").fetchall()
print("站点总数:", len(rows))
for r in rows:
    s = str(r)
    if 'travel' in s.lower():
        print("HIT:", r)
print("全部站点名:")
for r in rows:
    print(" -", r)
EOF
echo "=== nginx vhost 中 travel ==="
ls /www/server/panel/vhost/nginx/ 2>/dev/null | grep -i travel || echo "无travel vhost conf"
echo "=== DONE ==="
