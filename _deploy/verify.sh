#!/bin/bash
echo "== health =="
curl -s -o /dev/null -w "%{http_code} " http://127.0.0.1:18083/api/actuator/health
curl -s http://127.0.0.1:18083/api/actuator/health; echo
echo "== site host =="
for d in lvyou.quantaida.cloud lvyou-admin.quantaida.cloud; do
  code=$(curl -s -o /tmp/travel/page.html -w "%{http_code}" -H "Host: $d" http://127.0.0.1/)
  echo "$d HTTP=$code TITLE=$(grep -o '<title>[^<]*</title>' /tmp/travel/page.html | head -1)"
done
echo "== api via nginx =="
curl -s -o /dev/null -w "health=%{http_code}\n" -H "Host: lvyou.quantaida.cloud" http://127.0.0.1/api/actuator/health
echo "== static asset =="
asset=$(ls /www/wwwroot/lvyou.quantaida.cloud/assets | head -1)
curl -s -o /dev/null -w "asset=$asset %{http_code}\n" -H "Host: lvyou.quantaida.cloud" "http://127.0.0.1/assets/$asset"
asset2=$(ls /www/wwwroot/lvyou-admin.quantaida.cloud/assets | head -1)
curl -s -o /dev/null -w "adminasset=$asset2 %{http_code}\n" -H "Host: lvyou-admin.quantaida.cloud" "http://127.0.0.1/assets/$asset2"
echo "== public https =="
for d in lvyou.quantaida.cloud lvyou-admin.quantaida.cloud; do
  curl -s -o /dev/null -w "$d %{http_code}\n" "https://$d/"
done
curl -s -o /dev/null -w "https api %{http_code}\n" https://lvyou.quantaida.cloud/api/actuator/health
echo "== login platform =="
curl -s -X POST -H "Content-Type: application/json" --data-binary @/tmp/travel/login_platform.json http://127.0.0.1:18083/api/auth/login | head -c 300; echo
echo "== login tenant HLJ =="
curl -s -X POST -H "Content-Type: application/json" --data-binary @/tmp/travel/login_tenant.json http://127.0.0.1:18083/api/auth/login | head -c 300; echo
echo "== upload dir =="
ls -ld /home/app/lvyou.quantaida.cloud/data/upload
