#!/usr/bin/env bash
DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"
clear
echo "===================================================================="
echo "            💳 CardMaster - Control Financiero Personal (macOS)"
echo "===================================================================="
echo ""

if command -v node >/dev/null 2>&1; then
    if [ ! -d "node_modules" ]; then
        echo "[1/2] Instalando componentes por primera vez..."
        npm install --no-audit --no-fund
    fi
    echo "[2/2] Abriendo CardMaster en http://localhost:3000..."
    open "http://localhost:3000" 2>/dev/null || true
    node server.js
else
    echo "[AVISO] Node.js no está instalado."
    echo "Abriendo CardMaster directamente en tu navegador Safari..."
    open "index.html"
fi

