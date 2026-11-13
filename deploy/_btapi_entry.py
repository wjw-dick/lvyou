import hashlib, time, urllib.parse, urllib.request, json, sys, ssl

API_SK = "qqWzUZMZE8bNA0wLlilRWlbG46HgQCMv"
BASE = "https://172.16.28.248:15284/00570ea3"

def md5(s):
    return hashlib.md5(s.encode('utf-8')).hexdigest()

def sign():
    rt = int(time.time())
    token = md5(str(rt) + md5(API_SK))
    return rt, token

def call(action_path, params):
    rt, token = sign()
    data = dict(params)
    data["request_time"] = rt
    data["request_token"] = token
    body = urllib.parse.urlencode(data).encode('utf-8')
    req = urllib.request.Request(BASE + action_path, data=body, method='POST')
    req.add_header('User-Agent', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36')
    req.add_header('Referer', BASE + '/')
    req.add_header('Content-Type', 'application/x-www-form-urlencoded')
    ctx = ssl._create_unverified_context()
    with urllib.request.urlopen(req, context=ctx, timeout=20) as resp:
        return resp.read().decode('utf-8')

def addsite(domain, path, ps):
    webname = json.dumps({"domain": domain, "domainlist": [], "count": 0}, ensure_ascii=False)
    params = {
        "webname": webname,
        "path": path,
        "type_id": "0",
        "type": "PHP",
        "version": "00",
        "port": "80",
        "ps": ps,
    }
    return call("/site?action=AddSite", params)

if __name__ == '__main__':
    cmd = sys.argv[1]
    if cmd == "addsite":
        print(addsite(sys.argv[2], sys.argv[3], sys.argv[4]))
    elif cmd == "getsitetotal":
        print(call("/system?action=GetSystemTotal", {}))
    elif cmd == "raw":
        print(call(sys.argv[2], json.loads(sys.argv[3])))
    else:
        print("unknown cmd")
