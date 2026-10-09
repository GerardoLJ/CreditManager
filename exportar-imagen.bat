@echo off
chcp 65001 >nul
title Exportar Imagen CreditManager

echo ==============================================================
echo   📦 Construyendo y exportando imagen de CreditManager
echo ==============================================================
echo.

docker build -t creditmanager:latest .
echo.
echo [INFO] Guardando imagen en creditmanager-image.tar...
docker save -o creditmanager-image.tar creditmanager:latest

echo.
echo ==============================================================
echo   ✅ ¡Imagen empaquetada como creditmanager-image.tar!
echo ==============================================================
echo   Sube este archivo a Google Drive, OneDrive o tu nube.
echo   En cualquier otra computadora:
echo     1. docker load -i creditmanager-image.tar
echo     2. docker run -d -p 3000:3000 -v C:\TusDatos:/data creditmanager:latest
echo ==============================================================
echo.
pause

