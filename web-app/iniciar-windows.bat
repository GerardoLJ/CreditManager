@echo off
title CardMaster - Aplicación Web
chcp 65001 > nul
cls
echo ====================================================================
echo             💳 CardMaster - Control Financiero Personal
echo ====================================================================
echo.

cd /d "%~dp0"

where node >nul 2>nul
if %errorlevel% neq 0 (
    echo [AVISO] Node.js no está instalado en este equipo Windows.
    echo.
    echo Abriendo CardMaster directamente en tu navegador web
    echo en modo autónomo con almacenamiento local...
    echo.
    timeout /t 2 > nul
    start "" "public\index.html"
    pause
    exit /b 0
)

if not exist "node_modules\" (
    echo [1/2] Configurando componentes por primera vez...
    call npm install --no-audit --no-fund
)

echo [2/2] Iniciando CardMaster en http://localhost:3000...
echo.
echo Presiona Ctrl + C o cierra esta ventana cuando desees detener la app.
echo ====================================================================

timeout /t 1 > nul
start "" "http://localhost:3000"
node server.js
