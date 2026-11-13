#!/bin/bash
# travel 后端启动脚本（JDK17 + 端口 18084）
export JAVA_HOME=/usr/lib/jvm/jdk-17
export PATH=$JAVA_HOME/bin:$PATH
export SPRING_PROFILES_ACTIVE=local
export BACKEND_PORT=18086
export DB_HOST=172.16.28.248
export MYSQL_PORT=3306
export DB_NAME=travel
export DB_USER=travel
export DB_PASSWORD=xRfJYJpzTZz8JEJj
export REDIS_HOST=172.16.28.201
export REDIS_PORT=6379
export REDIS_PASSWORD=RUiTMHWVr5cdoAPg
export RABBIT_HOST=172.16.28.201
export RABBIT_PORT=5672
export RABBIT_USER=aicadmin
export RABBIT_PASSWORD=admin9695
export MINIO_HOST=172.16.28.201
export MINIO_USER=admin
export MINIO_PASSWORD=oSSW1dgChuqNxSlh
export MINIO_PORT=9000
export JWT_SECRET='travel-platform-jwt-secret-change-me-2026-32chars!'

APP_DIR=/home/app/travel.jxdg.net
JAR=$APP_DIR/travel-platform.jar
LOG=$APP_DIR/travel.log
PID_FILE=$APP_DIR/app.pid

# 若已在运行则先停
if [ -f "$PID_FILE" ]; then
  OLD_PID=$(cat "$PID_FILE")
  if kill -0 "$OLD_PID" 2>/dev/null; then
    echo "旧进程 $OLD_PID 仍在运行，先停止"
    kill "$OLD_PID"
    sleep 5
  fi
fi

echo "启动 travel 后端 (JDK17, port=18086)..."
nohup $JAVA_HOME/bin/java -Xms512m -Xmx1024m -jar "$JAR" > "$LOG" 2>&1 &
echo $! > "$PID_FILE"
echo "PID=$(cat $PID_FILE)"
echo "启动命令已执行，日志: $LOG"
