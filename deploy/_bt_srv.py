#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# 服务器端宝塔API调用：从面板读取 api_sk，本地计算签名，密钥不回显
import hashlib, time, urllib.parse, urllib.request, ssl, json, sys, os, re, glob

def md5(s):
    return hashlib.md5(s.encode('utf-8')).hexdigest()

CANDIDATES = [
    '/www/server/panel/default.pl',
    '/www/server/panel/config/api_config.py',
    '/www/server/panel/data/default.pl',
]

def find_api_sk():
    pat = re.compile(r"['\"]?api_sk['\"]?\s*[:=|]\s*['\"]?([A-Za-z0-9]{10,64})", re.I)
    for path in CANDIDATES:
        if not os.path.exists(path):
            continue
        try:
            with open(path, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()
        except Exception:
            continue
        m = pat.search(content)
        if m:
            return m.group(1)
    # 面板 v8: sqlite default.db config 表
    try:
        import sqlite3
        conn = sqlite3.connect('/www/server/panel/data/default.db')
        cur = conn.execute("SELECT value FROM config WHERE id='api_sk'")
        row = cur.fetchone()
        conn.close()
        if row and row[0]:
            return str(row[0]).strip()
    except Exception:
        pass
    # 兜底：在面板目录里搜含 api_sk 的文件
    for path in glob.glob('/www/server/panel/**/*.py', recursive=True) + glob.glob('/www/server/panel/**/*.pl', recursive=True):
        try:
            with open(path, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()
        except Exception:
            continue
        if 'api_sk' not in content:
            continue
        m = pat.search(content)
        if m:
            return m.group(1)
    return None

def call(path, params, entry):
    sk = find_api_sk()
    if not sk:
        raise SystemExit('API_SK_NOT_FOUND')
    rt = int(time.time())
    token = md5(str(rt) + md5(sk))
    data = dict(params)
    data['request_time'] = rt
    data['request_token'] = token
    body = urllib.parse.urlencode(data).encode('utf-8')
    url = 'https://127.0.0.1:15284' + entry + path
    req = urllib.request.Request(url, data=body, method='POST')
    req.add_header('User-Agent', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36')
    req.add_header('Referer', 'https://127.0.0.1:15284' + entry + '/')
    req.add_header('Content-Type', 'application/x-www-form-urlencoded')
    ctx = ssl._create_unverified_context()
    with urllib.request.urlopen(req, context=ctx, timeout=25) as r:
        return r.read().decode('utf-8')

if __name__ == '__main__':
    entry = '/00570ea3'
    cmd = sys.argv[1]
    if cmd == 'test':
        print(call('/system?action=GetSystemTotal', {}, entry))
    elif cmd == 'addsite':
        domain, path, ps = sys.argv[2], sys.argv[3], sys.argv[4]
        webname = json.dumps({'domain': domain, 'domainlist': [], 'count': 0}, ensure_ascii=False)
        params = {'webname': webname, 'path': path, 'type_id': '0', 'type': 'PHP', 'version': '00', 'port': '80', 'ps': ps}
        print(call('/site?action=AddSite', params, entry))
    elif cmd == 'sites':
        print(call('/data?action=getData&table=sites', {}, entry))
    else:
        print('unknown cmd')
