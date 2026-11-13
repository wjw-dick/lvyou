#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import sqlite3
conn = sqlite3.connect('/www/server/panel/data/default.db')
tables = [r[0] for r in conn.execute("SELECT name FROM sqlite_master WHERE type='table'")]
print('TABLES:', tables)
for t in tables:
    try:
        cols = [c[1] for c in conn.execute("PRAGMA table_info('%s')" % t)]
        print('COLS', t, cols)
    except Exception as e:
        print('ERR', t, e)
# 只列 config 类表的键名（不读值）
for t in tables:
    try:
        rows = conn.execute("SELECT * FROM '%s' LIMIT 1" % t)
        print('SAMPLE', t, [c[0] for c in rows.description])
    except Exception as e:
        pass
conn.close()
print('DONE')
