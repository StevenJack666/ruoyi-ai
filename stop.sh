#!/bin/bash

# RuoYi-AI 应用停止脚本

# 获取脚本所在目录
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
cd "$SCRIPT_DIR"

# 定义配置
LOG_DIR="./logs"
PID_FILE="$LOG_DIR/ruoyi-ai.pid"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查pid文件
if [ ! -f "$PID_FILE" ]; then
    echo -e "${YELLOW}⚠ 应用未运行（找不到进程文件）${NC}"
    exit 0
fi

PID=$(cat "$PID_FILE")

# 检查进程是否存在
if ! kill -0 "$PID" 2>/dev/null; then
    echo -e "${YELLOW}⚠ 应用未运行（进程 $PID 不存在）${NC}"
    rm -f "$PID_FILE"
    exit 0
fi

# 停止应用
echo -e "${YELLOW}正在停止应用...${NC}"
kill "$PID"

# 等待应用停止
sleep 2

# 如果还没停止，强制杀死
if kill -0 "$PID" 2>/dev/null; then
    echo -e "${YELLOW}应用未停止，发送强制信号...${NC}"
    kill -9 "$PID"
    sleep 1
fi

# 验证进程已停止
if ! kill -0 "$PID" 2>/dev/null; then
    echo -e "${GREEN}✓ 应用已停止${NC}"
    rm -f "$PID_FILE"
else
    echo -e "${RED}✗ 应用停止失败${NC}"
    exit 1
fi
