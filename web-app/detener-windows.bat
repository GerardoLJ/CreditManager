@echo off
title Detener CardMaster
chcp 65001 > nul
echo Deteniendo CardMaster en el puerto 3000...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":3000" ^| findstr "LISTENING"') do taskkill /f /pid %%a >nul 2>nul
echo ✅ CardMaster detenido correctamente.
timeout /t 2 > nul
