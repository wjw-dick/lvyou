#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import sqlite3, os, glob
print('SITES_DB:')
try:
    conn = sqlite3.connect('/www/server/panel/data/db/site.db')
    for row in conn.execute("SELECT id,name,path,status,ps FROM sites"):
        print(row)
    print('DOMAIN:')
    for row in conn.execute("SELECT id,pid,name,port FROM domain"):
        print(row)
    conn.close()
except Exception as e:
    print('ERR', e)
print('VHOST_CONF:')
for f in sorted(glob.glob('/www/server/panel/vhost/nginx/*travel*')):
    print(f, os.path.getsize(f))
print('DONE')
