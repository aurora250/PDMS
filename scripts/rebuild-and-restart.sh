#!/bin/bash
set -e
# ============================================================
# PDM 快速重新编译并重启脚本
# 用途: 编译所有模块，重新构建镜像，重新启动服务
# 用法: ./scripts/rebuild-and-restart.sh [--reset-db]
#   --reset-db  同时重置数据库
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

RESET_DB=false
if [ "$1" = "--reset-db" ]; then
    RESET_DB=true
fi

echo "============================================================"
echo " PDM 重新编译并重启"
echo "============================================================"
echo ""

# Step 1: 编译
echo "[1/4] 编译所有模块..."
mvn clean package -DskipTests -q
echo "  [OK] 编译完成"

# Step 2: 停止
echo "[2/4] 停止现有服务..."
docker compose down
echo "  [OK] 服务已停止"

# Step 3: 数据库重置（可选）
if [ "$RESET_DB" = true ]; then
    echo "[3/4] 重置数据库..."
    docker volume rm peopledatabasemanagement-linux_pdm-postgresql-data 2>/dev/null || true
    docker volume rm peopledatabasemanagement-linux_pdm-es-data 2>/dev/null || true
    docker volume rm peopledatabasemanagement-linux_pdm-redis-data 2>/dev/null || true
    echo "  [OK] 数据库数据已清除"
else
    echo "[3/4] 保留现有数据库数据（使用 --reset-db 参数以清除数据）"
fi

# Step 4: 构建并启动
echo "[4/4] 构建镜像并启动..."
docker compose up -d --build

echo ""
echo "============================================================"
echo " 等待服务就绪..."
echo "============================================================"

# 等待关键服务
sleep 5
for svc in pdm-postgresql pdm-redis pdm-elasticsearch pdm-nacos; do
    if docker ps --format '{{.Names}}' | grep -q "$svc"; then
        echo "  [OK] $svc 运行中"
    else
        echo "  [WARN] $svc 可能未启动"
    fi
done

echo ""
echo "============================================================"
echo " 所有操作完成!"
echo " Gateway: http://localhost:8080"
echo "============================================================"
