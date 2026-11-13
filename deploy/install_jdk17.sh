#!/bin/bash
set -e
echo "=== 安装 JDK17 (清华镜像) ==="
JDK_DIR=/usr/lib/jvm/jdk-17
TUNA_URL="https://mirrors.tuna.tsinghua.edu.cn/Adoptium/17/jdk/x64/linux/OpenJDK17U-jdk_x64_linux_hotspot_17.0.20.1_1.tar.gz"
if [ -x "$JDK_DIR/bin/java" ]; then
  echo "JDK17 已存在，跳过下载"
else
  echo "下载 JDK17 从清华镜像..."
  curl -sL --max-time 180 -o /tmp/jdk17.tar.gz "$TUNA_URL"
  ls -lh /tmp/jdk17.tar.gz
  file /tmp/jdk17.tar.gz 2>/dev/null || true
  if [ -s /tmp/jdk17.tar.gz ]; then
    echo "解压到 $JDK_DIR"
    mkdir -p "$JDK_DIR"
    tar -xzf /tmp/jdk17.tar.gz -C "$JDK_DIR" --strip-components=1
  else
    echo "下载失败"
    exit 1
  fi
fi
echo "=== JDK17 版本 ==="
"$JDK_DIR/bin/java" -version 2>&1
echo "ALL_DONE"
