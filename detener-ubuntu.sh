#!/usr/bin/env bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Si existe server.pid, matar ese proceso
if [ -f "$DIR/server.pid" ]; then
    PID=$(cat "$DIR/server.pid")
    if kill -0 "$PID" 2>/dev/null; then
        kill "$PID" 2>/dev/null || true
    fi
    rm -f "$DIR/server.pid"
fi

# Por seguridad, asegurar que no quede ningún node server.js en este directorio
pkill -f "node server.js" 2>/dev/null || true

echo "✅ CardMaster se ha cerrado correctamente."

