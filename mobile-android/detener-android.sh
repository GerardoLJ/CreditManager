#!/usr/bin/env bash
# ==============================================================
#  CardMaster - Detener Servidor para Android
# ==============================================================

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ -f "$DIR/server.pid" ]; then
    PID=$(cat "$DIR/server.pid")
    if kill -0 "$PID" 2>/dev/null; then
        kill "$PID" 2>/dev/null || true
        echo "✅ Servidor móvil detenido (PID $PID)."
    fi
    rm -f "$DIR/server.pid"
else
    pkill -f "node server.js" 2>/dev/null || true
    echo "✅ Servidor detenido."
fi

