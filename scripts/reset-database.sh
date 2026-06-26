#!/bin/bash
set -e
# ============================================================
# PDM 数据库重置脚本
# 用途: 清空所有数据库数据并重新初始化
# 用法: ./scripts/reset-database.sh [--hard]
#   --hard  同时删除所有 Docker 镜像并重建（完全从头开始）
# ============================================================

# 自动检测 docker 命令（Windows Git Bash 下 Docker 可能只在 WSL 内）
if ! command -v docker &>/dev/null && command -v wsl &>/dev/null; then
    docker() { wsl docker "$@"; }
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

HARD_RESET=false
if [ "$1" = "--hard" ]; then
    HARD_RESET=true
fi

echo "============================================================"
echo " PDM 数据库重置脚本"
echo " 项目目录: $PROJECT_DIR"
echo " 模式: $([ "$HARD_RESET" = true ] && echo '完全重建' || echo '仅重置数据')"
echo "============================================================"
echo ""

# Step 1: 停止所有容器
echo "[1/5] 停止所有容器..."
docker compose down 2>/dev/null || true
echo "  [OK] 容器已停止"

# Step 2: 删除数据库相关数据卷
echo "[2/5] 删除数据库数据卷..."
docker compose down -v 2>/dev/null || true
echo "  [OK] 数据卷已删除"

# Step 3: 清理孤儿容器和网络（可选）
echo "[3/5] 清理 Docker 资源..."
docker container prune -f 2>/dev/null || true
docker network prune -f 2>/dev/null || true
echo "  [OK] Docker 资源已清理"

# Step 4: (hard mode only) 重新编译并重建镜像
if [ "$HARD_RESET" = true ]; then
    echo "[4/5] 重新编译并重建镜像..."
    echo "  Maven 编译中（跳过测试）..."
    mvn clean package -DskipTests -q
    echo "  [OK] Maven 编译完成"
    echo "  清理本项目旧镜像..."
    docker compose down --rmi local 2>/dev/null || true
    echo "  [OK] 旧镜像已清理"
else
    echo "[4/5] 跳过镜像重建（使用 --hard 参数以重新编译）"
fi

# Step 5: 启动所有服务
echo "[5/5] 启动所有服务..."
if [ "$HARD_RESET" = true ]; then
    docker compose up -d --build
else
    docker compose up -d
fi

echo ""
echo "============================================================"
echo " 服务启动中，等待就绪..."
echo "============================================================"

# 等待 PostgreSQL 就绪
TIMEOUT=120
ELAPSED=0
while [ $ELAPSED -lt $TIMEOUT ]; do
    if docker exec pdm-postgresql pg_isready -U pdm -d pdm_db &>/dev/null; then
        echo "  [OK] PostgreSQL 已就绪 (${ELAPSED}s)"
        break
    fi
    sleep 2
    ELAPSED=$((ELAPSED + 2))
done

if [ $ELAPSED -ge $TIMEOUT ]; then
    echo "  [ERROR] PostgreSQL 启动超时"
    exit 1
fi

# 等待 PgBouncer 就绪
sleep 3
if docker exec pdm-pgbouncer pg_isready -U pdm -h 127.0.0.1 -p 6432 -d pdm_db &>/dev/null 2>&1 || true; then
    echo "  [OK] PgBouncer 已就绪"
else
    echo "  [WARN] PgBouncer 健康检查不支持，跳过（服务将直连 PostgreSQL）"
fi

# 验证数据库和表
echo ""
echo "============================================================"
echo " 验证数据库初始化..."
echo "============================================================"

echo "  主数据库 (pdm_db) 表:"
docker exec pdm-postgresql psql -U pdm -d pdm_db -c "\dt" 2>/dev/null | tail -n +3 | head -30

echo ""
echo "  分片数据库表:"
for db in pdm_shard_0 pdm_shard_1 pdm_shard_2 pdm_shard_3; do
    TABLE_COUNT=$(docker exec pdm-postgresql psql -U pdm -d "$db" -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public'" 2>/dev/null | tr -d ' ')
    echo "    $db: $TABLE_COUNT tables"
done

# 验证 area 表数据
echo ""
echo "  区域数据验证:"
AREA_COUNT=$(docker exec pdm-postgresql psql -U pdm -d pdm_db -t -c "SELECT COUNT(*) FROM area" 2>/dev/null | tr -d ' ')
echo "    pdm_db.area: $AREA_COUNT records"

# 验证物化视图
echo ""
echo "  物化视图:"
docker exec pdm-postgresql psql -U pdm -d pdm_db -c "\dm" 2>/dev/null || echo "    (无物化视图或当前用户无权限)"

echo ""
echo "============================================================"
echo " 数据库重置完成!"
echo ""
echo " 服务访问地址:"
echo "   Gateway:  http://localhost:8080"
echo "   Nginx:    http://localhost:18080"
echo "   Nacos:    http://localhost:8848/nacos"
echo "   PG:       localhost:15432 (pdm/pdm123)"
echo "   PgBouncer: localhost:6432 (连接池)"
echo "   Redis:    localhost:16379"
echo "   ES:       http://localhost:9200"
echo ""
echo " 管理命令:"
echo "   查看日志: docker compose logs -f [service-name]"
echo "   停止服务: docker compose down"
echo "   重置数据: ./scripts/reset-database.sh"
echo "   完全重建: ./scripts/reset-database.sh --hard"
echo "============================================================"
