@echo off
cd /d "%~dp0.."
echo Starting PDM all services...
docker compose up -d
echo Done!
pause
