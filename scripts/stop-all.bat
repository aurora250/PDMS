@echo off
cd /d "%~dp0.."
echo Stopping PDM all containers...
docker compose down
echo Done! Data volumes preserved.
echo Use: docker compose down -v   to also clear data
pause
