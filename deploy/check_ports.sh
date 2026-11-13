#!/bin/bash
echo "=== 检查 18084/18085/18086/18087/8081 占用 ==="
for p in 18084 18085 18086 18087 8081 18090; do
  if netstat -tlnp 2>/dev/null | grep ":$p " >/dev/null; then
    pid=$(netstat -tlnp 2>/dev/null | grep ":$p " | grep -oE 'pid=[0-9]+' | head -1)
    echo "$p: 占用 $pid"
  else
    echo "$p: 空闲"
  fi
done
echo "=== 18085 详细 ==="
netstat -tlnp 2>/dev/null | grep ':18085' || echo "18085 空闲"
echo "DONE"
