#!/bin/bash
echo "== 服务器上公网真实域名 portal =="
curl -s --max-time 15 -w '\ncode=%{http_code} ip=%{remote_ip}\n' -o /tmp/portal_page.html "http://travel.jxdg.net/" 2>&1
echo "-- 页面标题 --"
grep -o '<title>[^<]*</title>' /tmp/portal_page.html 2>/dev/null | head -1
echo "== 服务器上公网真实域名 admin =="
curl -s --max-time 15 -w '\ncode=%{http_code} ip=%{remote_ip}\n' -o /tmp/admin_page.html "http://travel-admin.jxdg.net/" 2>&1
echo "-- 页面标题 --"
grep -o '<title>[^<]*</title>' /tmp/admin_page.html 2>/dev/null | head -1
echo "== 直连公网IP(绕过CF) portal =="
curl -s --max-time 15 -w '\ncode=%{http_code}\n' -o /dev/null "http://111.40.45.8/" -H "Host: travel.jxdg.net" 2>&1
echo "== 直连公网IP(绕过CF) admin =="
curl -s --max-time 15 -w '\ncode=%{http_code}\n' -o /dev/null "http://111.40.45.8/" -H "Host: travel-admin.jxdg.net" 2>&1
echo "== DONE =="
