# -*- coding: utf-8 -*-
import subprocess
urls = [
    "http://travel.jxdg.net/",
    "http://travel.jxdg.net/api/actuator/health",
    "http://travel-admin.jxdg.net/",
]
for u in urls:
    try:
        r = subprocess.run(
            ["curl.exe", "-s", "--max-time", "15", "-o", "NUL", "-w", "code=%{http_code}", "-H", "User-Agent: Mozilla/5.0", "-H", "Accept: text/html", u],
            capture_output=True, text=True, timeout=20)
        print(u, "| rc=", r.returncode, "| out=", r.stdout.strip(), "| err=", r.stderr.strip()[:300])
    except Exception as e:
        print(u, "| EXC", repr(e))
