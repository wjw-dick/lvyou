#!/bin/bash
echo "=== 清华 Adoptium 17 目录列表 ==="
curl -s --max-time 20 "https://mirrors.tuna.tsinghua.edu.cn/Adoptium/17/jdk/x64/linux/" 2>&1 | grep -oE 'OpenJDK17U-jdk[^"<]*\.tar\.gz' | sort -u | head -20
echo "DONE"
