@echo off
chcp 65001 >nul
title CardMaster - CreditManager

echo.
echo ==============================================================
echo     💳 CardMaster / CreditManager - Inicio en Windows
echo ==============================================================
echo.

docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker Desktop no está instalado o no se encuentra activo.
    echo Por favor abre o instala Docker Desktop desde: https://www.docker.com/products/docker-desktop/
    echo.
    pause
    exit /b 1
)

echo Selecciona la carpeta de esta computadora donde se guardarán los datos (tarjetas.db):
echo  - Presiona [ENTER] para usar la carpeta por defecto (.\data en esta carpeta)
echo  - O escribe una ruta completa (ejemplo: C:\MisFinanzas o D:\Datos)
echo.
set /p RUTA_INPUT="Carpeta de datos [.\data]: "

if "%RUTA_INPUT%"=="" set RUTA_INPUT=.\data

if not exist "%RUTA_INPUT%" mkdir "%RUTA_INPUT%"

for %%I in ("%RUTA_INPUT%") do set RUTA_ABS=%%~fI

echo.
echo [INFO] Carpeta de datos seleccionada: %RUTA_ABS%
echo [INFO] Iniciando contenedor...

docker stop creditmanager >nul 2>&1
docker rm creditmanager >nul 2>&1

docker images -q creditmanager:latest | findstr . >nul 2>&1
if %errorlevel% neq 0 (
    echo [INFO] Construyendo imagen de CreditManager por primera vez...
    docker build -t creditmanager:latest .
)

docker run -d --name creditmanager --restart unless-stopped -p 3000:3000 -v "%RUTA_ABS%:/data" creditmanager:latest

echo.
echo ==============================================================
echo   🎉 ¡CreditManager está funcionando con éxito!
echo ==============================================================
echo   💻 Abre en tu navegador: http://localhost:3000
echo   📁 Base de datos física en: %RUTA_ABS%\tarjetas.db
echo ==============================================================
echo.

start http://localhost:3000
echo Para detener la aplicación, haz doble clic en detener.bat
pause

