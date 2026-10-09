@echo off
chcp 65001 >nul
title Detener CreditManager

echo Deteniendo contenedor CreditManager...
docker stop creditmanager >nul 2>&1
docker rm creditmanager >nul 2>&1
echo [OK] CreditManager detenido correctamente. Tus datos están intactos en tu carpeta seleccionada.
pause

