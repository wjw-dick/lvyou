#!/bin/bash
echo "=== 测试各JDK源可达性 ==="
for url in \
  "https://mirrors.tuna.tsinghua.edu.cn/Adoptium/17/jdk/x64/linux/" \
  "https://mirrors.huaweicloud.com/openjdk/" \
  "https://download.java.net/java/GA/jdk17.0.2/dfd4a8d0985749f896bed50d7138ee7/8/GPL/openjdk-17.0.2_linux-x64_bin.tar.gz" \
  "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.21%2B9/OpenJDK17U-jdk_x64_linux_hotspot_17.0.21_9.tar.gz" \
  ; do
  code=$(curl -sI --max-time 12 -o /dev/null -w '%{http_code}' "$url" 2>/dev/null)
  echo "$code  <-  $url"
done
echo "DONE"
