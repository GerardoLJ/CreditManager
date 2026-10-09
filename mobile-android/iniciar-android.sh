#!/usr/bin/env bash
# ==============================================================
#  CardMaster - Servidor Autónomo para Dispositivos Móviles (Android)
# ==============================================================

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

# Obtener IP local de la máquina en la red Wi-Fi
LOCAL_IP=$(hostname -I 2>/dev/null | awk '{print $1}')
if [ -z "$LOCAL_IP" ]; then
    LOCAL_IP="127.0.0.1"
fi

PORT=3000
SERVER_URL="http://${LOCAL_IP}:${PORT}"

echo "=============================================================="
echo "    📱 CardMaster / CreditManager — Servidor para Android"
echo "=============================================================="
echo ""
echo "  🌐 URL para tu Celular: $SERVER_URL"
echo ""
echo "  Instrucciones para tu teléfono Android:"
echo "  1. Asegúrate de que tu celular esté conectado a la misma red Wi-Fi."
echo "  2. Abre Chrome o Firefox en tu celular y entra a:"
echo "     👉  $SERVER_URL"
echo "  3. En el menú de opciones del navegador (tres puntos), toca:"
echo "     📲 'Instalar aplicación' o 'Agregar a la pantalla principal'."
echo "  4. ¡Listo! Se creará el icono de CardMaster en tu teléfono"
echo "     y funcionará a pantalla completa como una app nativa."
echo "=============================================================="
echo ""

# Iniciar servidor Node.js
if [ -f "$DIR/server.pid" ]; then
    OLD_PID=$(cat "$DIR/server.pid")
    if kill -0 "$OLD_PID" 2>/dev/null; then
        echo "ℹ️ El servidor móvil ya estaba corriendo (PID $OLD_PID)."
        exit 0
    fi
fi

node server.js &
SERVER_PID=$!
echo "$SERVER_PID" > "$DIR/server.pid"
echo "✅ Servidor activo en segundo plano (PID $SERVER_PID)."
echo "   Para detenerlo en cualquier momento ejecuta: ./detener-android.sh"

