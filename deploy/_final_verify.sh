#!/bin/bash
echo "== 1. portal 主页 (Host travel.jxdg.net) =="
curl -s --max-time 10 -o /dev/null -w 'code=%{http_code}\n' http://127.0.0.1/ -H "Host: travel.jxdg.net"
curl -s --max-time 10 http://127.0.0.1/ -H "Host: travel.jxdg.net" | grep -o '<title>[^<]*</title>' | head -1
echo "== 2. admin 主页 (Host travel-admin.jxdg.net) =="
curl -s --max-time 10 -o /dev/null -w 'code=%{http_code}\n' http://127.0.0.1/ -H "Host: travel-admin.jxdg.net"
curl -s --max-time 10 http://127.0.0.1/ -H "Host: travel-admin.jxdg.net" | grep -o '<title>[^<]*</title>' | head -1
echo "== 3. API 反代 (Host travel.jxdg.net /api/actuator/health) =="
curl -s --max-time 10 -w '\ncode=%{http_code}\n' http://127.0.0.1/api/actuator/health -H "Host: travel.jxdg.net"
echo "== 4. API 反代 (Host travel-admin.jxdg.net /api/actuator/health) =="
curl -s --max-time 10 -w '\ncode=%{http_code}\n' http://127.0.0.1/api/actuator/health -H "Host: travel-admin.jxdg.net"
echo "== 5. 后端直连 18086 =="
curl -s --max-time 10 -w '\ncode=%{http_code}\n' http://127.0.0.1:18086/api/actuator/health
echo "== DONE =="
