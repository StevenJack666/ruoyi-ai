#!/bin/bash

# RuoYi-AI 应用状态查看脚本

# 获取脚本所在目录
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
cd "$SCRIPT_DIR"

# 定义配置
LOG_DIR="./logs"
LOG_FILE="$LOG_DIR/ruoyi-ai.log"
PID_FILE="$LOG_DIR/ruoyi-ai.pid"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========== RuoYi-AI 应用状态 ==========${NC}"

# 检查进程状态
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if kill -0 "$PID" 2>/dev/null; then
        echo -e "${GREEN}✓ 应用运行中${NC}"
        echo "进程ID: $PID"
        echo "启动命令: $(ps -p $PID -o cmd=)"
        
        # 获取内存占用
        MEM=$(ps -p $PID -o rss=)
        echo "内存占用: $(echo "scale=2; $MEM / 1024" | bc) MB"
        
        # 获取CPU占用
        CPU=$(ps -p $PID -o %cpu=)
        echo "CPU占用: $CPU%"
    else
        echo -e "${RED}✗ 应用已停止${NC}"
        echo "进程ID: $PID（已不存在）"
        rm -f "$PID_FILE"
    fi
else
    echo -e "${RED}✗ 应用未运行${NC}"
fi

echo ""
echo -e "${BLUE}========== 应用日志 ==========${NC}"

if [ ! -f "$LOG_FILE" ]; then
    echo -e "${YELLOW}日志文件不存在${NC}"
else
    echo "日志文件: $LOG_FILE"
    echo ""
    echo "最近 20 行日志:"
    echo "---"
    tail -20 "$LOG_FILE"
    echo "---"
    echo ""
    echo "实时查看日志: tail -f $LOG_FILE"
    echo "查看所有日志: cat $LOG_FILE"
fi

echo ""
echo -e "${BLUE}========== 常用命令 ==========${NC}"
echo "启动应用:   ./start.sh"
echo "停止应用:   ./stop.sh"
echo "查看状态:   ./status.sh"
echo "查看日志:   tail -f ./logs/ruoyi-ai.log"
