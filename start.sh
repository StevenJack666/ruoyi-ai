#!/bin/bash

# RuoYi-AI 应用启动脚本

# 获取脚本所在目录
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
cd "$SCRIPT_DIR"

# 定义应用名称和配置
APP_NAME="ruoyi-admin"
JAR_FILE="./ruoyi-admin/target/ruoyi-admin.jar"
LOG_DIR="./logs"
LOG_FILE="$LOG_DIR/ruoyi-ai.log"
PID_FILE="$LOG_DIR/ruoyi-ai.pid"

# 创建日志目录
mkdir -p "$LOG_DIR"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查jar文件是否存在
if [ ! -f "$JAR_FILE" ]; then
    echo -e "${RED}✗ 错误：找不到 $JAR_FILE${NC}"
    echo "请先执行: mvn clean package -DskipTests"
    exit 1
fi

# 检查是否已经运行
if [ -f "$PID_FILE" ]; then
    OLD_PID=$(cat "$PID_FILE")
    if kill -0 "$OLD_PID" 2>/dev/null; then
        echo -e "${YELLOW}⚠ 应用已在运行，进程ID: $OLD_PID${NC}"
        echo "如需重启，请先执行: ./stop.sh"
        exit 1
    fi
fi

# JVM 参数配置
JVM_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

# 启动应用
echo -e "${YELLOW}正在启动 $APP_NAME...${NC}"
nohup java $JVM_OPTS -jar "$JAR_FILE" \
    --spring.profiles.active=dev \
    >> "$LOG_FILE" 2>&1 &

# 获取进程ID
NEW_PID=$!
echo $NEW_PID > "$PID_FILE"

# 等待应用启动
sleep 3

# 检查应用是否成功启动
if kill -0 "$NEW_PID" 2>/dev/null; then
    echo -e "${GREEN}✓ 应用启动成功${NC}"
    echo "进程ID: $NEW_PID"
    echo "应用地址: http://localhost:6039"
    echo "日志文件: $LOG_FILE"
    echo ""
    echo "查看日志: tail -f $LOG_FILE"
    echo "停止应用: ./stop.sh"
else
    echo -e "${RED}✗ 应用启动失败${NC}"
    echo "请查看日志: tail -f $LOG_FILE"
    rm -f "$PID_FILE"
    exit 1
fi
