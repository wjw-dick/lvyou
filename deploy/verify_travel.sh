#!/bin/bash
{
echo "=== 1. 后端本机直连 18086 ==="
curl -s -o /dev/null -w 'backend_direct http=%{http_code}\n' --max-time 10 http://127.0.0.1:18086/api/actuator/health 2>&1 || echo "backend_direct 失败"
curl -s --max-time 10 http://127.0.0.1:18086/api/actuator/health 2>&1 | head -c 200
echo ""
echo "=== 2. 通过 nginx 域名访问后端 /api ==="
curl -s -o /dev/null -w 'nginx_api http=%{http_code}\n' --max-time 10 http://127.0.0.1/api/actuator/health -H "Host: travel.jxdg.net" 2>&1 || echo "nginx_api 失败"
echo "=== 3. 通过 nginx 域名访问门户根 ==="
curl -s -o /dev/null -w 'portal_root http=%{http_code}\n' --max-time 10 http://127.0.0.1/ -H "Host: travel.jxdg.net" 2>&1 || echo "portal_root 失败"
echo "=== 4. 通过 nginx 访问 admin ==="
curl -s -o /dev/null -w 'admin http=%{http_code}\n' --max-time 10 http://127.0.0.1/admin/ -H "Host: travel.jxdg.net" 2>&1 || echo "admin 失败"
echo "=== CHECK_DONE ==="
} > /tmp/verify_travel.log 2>&1
