#!/usr/bin/env bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"
clear
echo "===================================================================="
echo "            💳 CardMaster - Control Financiero Personal (Linux)"
echo "===================================================================="
echo ""

if command -v node >/dev/null 2>&1; then
    if [ ! -d "node_modules" ]; then
        echo "[1/2] Instalando dependencias por primera vez..."
        npm install --no-audit --no-fund
    fi
    echo "[2/2] Abriendo CardMaster en http://localhost:3000..."
    xdg-open "http://localhost:3000" 2>/dev/null || sensible-browser "http://localhost:3000" 2>/dev/null || open "http://localhost:3000" 2>/dev/null || true
    node server.js
else
    echo "Abriendo directamente en tu navegador..."
    xdg-open "$DIR/index.html" 2>/dev/null || sensible-browser "$DIR/index.html" 2>/dev/null || true
fi

