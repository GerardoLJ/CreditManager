#!/usr/bin/env bash
# ==============================================================
#  CardMaster — Detener todos los servicios
# ==============================================================

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Deteniendo servicios de CardMaster..."

# Detener escritorio si está activo
if [ -f "$DIR/desktop-ubuntu/detener-ubuntu.sh" ]; then
    bash "$DIR/desktop-ubuntu/detener-ubuntu.sh" 2>/dev/null || true
fi

# Detener móvil si está activo
if [ -f "$DIR/mobile-android/detener-android.sh" ]; then
    bash "$DIR/mobile-android/detener-android.sh" 2>/dev/null || true
fi

# Asegurar terminación de procesos node server.js huérfanos
pkill -f "node server.js" 2>/dev/null || true

echo "✅ Todos los servicios de CardMaster han sido detenidos correctamente."
