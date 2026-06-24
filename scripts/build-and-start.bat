@echo off
cd /d "%~dp0.."
chcp 65001 >/dev/null
title PDM Build and Start

set JAVA_HOME=D:\Java\jdk-21
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo ============================================================
echo  PDM - Step 1/3: Build all JARs
echo ============================================================
call mvn package -DskipTests -q
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Maven build failed!
    pause
    exit /b 1
)
echo [OK] JARs built

echo.
echo ============================================================
echo  PDM - Step 2/3: Build Docker images
echo ============================================================
wsl -- bash -c "cd /mnt/d/Workspace/Java\ Workspace/IDEA/PeopleDatabaseManagement && docker compose build"
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Docker build failed!
    pause
    exit /b 1
)
echo [OK] Images built

echo.
echo ============================================================
echo  PDM - Step 3/3: Start all services
echo ============================================================
echo  Stopping old containers first...
wsl -- bash -c "docker rm -f pdm-mysql pdm-redis pdm-elasticsearch pdm-nacos pdm-nginx pdm-gateway pdm-auth pdm-resident pdm-household pdm-keyperson pdm-floating-population pdm-missingperson pdm-log pdm-notification 2>/dev/null"
echo  Starting all services...
wsl -- bash -c "cd /mnt/d/Workspace/Java\ Workspace/IDEA/PeopleDatabaseManagement && docker compose up -d"
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Docker start failed!
    pause
    exit /b 1
)

echo.
echo ============================================================
echo  All services started!
echo    Gateway : http://localhost:8080
echo    Nginx   : http://localhost:18080
echo    Nacos   : http://localhost:8848/nacos
echo    ES      : http://localhost:9200
echo.
echo  Logs : wsl -- docker compose logs -f [service]
echo  Stop : wsl -- docker compose down
echo ============================================================
pause
