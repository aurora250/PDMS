#!/bin/bash
set -e

PROJECT_DIR="/mnt/d/Workspace/Java Workspace/IDEA/PeopleDatabaseManagement"
export JAVA_HOME="/mnt/d/Java/jdk-21"
export PATH="$JAVA_HOME/bin:$PATH"

cd "$PROJECT_DIR"

echo "============================================================"
echo " PDM - Step 1/3: Build all JARs"
echo "============================================================"
mvn package -DskipTests -q
echo "[OK] JARs built"

echo ""
echo "============================================================"
echo " PDM - Step 2/3: Build Docker images"
echo "============================================================"
docker compose build
echo "[OK] Images built"

echo ""
echo "============================================================"
echo " PDM - Step 3/3: Start all services"
echo "============================================================"
docker compose up -d

echo ""
echo "============================================================"
echo " All services started!"
echo "   Gateway : http://localhost:8080"
echo "   Nginx   : http://localhost:18080"
echo "   Nacos   : http://localhost:8848/nacos"
echo "   ES      : http://localhost:9200"
echo ""
echo " Logs : docker compose logs -f [service]"
echo " Stop : docker compose down"
echo "============================================================"
