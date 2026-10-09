#!/usr/bin/env bash
set -e

# ==============================================================
#  💳 CreditManager / CardMaster - Asistente de Inicio Portable
# ==============================================================

echo ""
echo "=============================================================="
echo "    💳 CardMaster / CreditManager - Inicio en este Equipo"
echo "=============================================================="
echo ""

# 1. Comprobar Docker y permisos
if ! command -v docker &> /dev/null; then
    echo "❌ Error: Docker no está instalado en este equipo."
    echo "👉 Descárgalo e instálalo gratis desde: https://www.docker.com/products/docker-desktop/"
    exit 1
fi

DOCKER_CMD="docker"
if ! docker ps >/dev/null 2>&1; then
    if command -v sudo &> /dev/null; then
        echo "ℹ️  Detectados permisos de administrador requeridos para Docker en Linux. Usando sudo..."
        DOCKER_CMD="sudo docker"
    else
        echo "❌ Permiso denegado al conectar con Docker (/var/run/docker.sock)."
        echo "👉 Para solucionarlo permanentemente, ejecuta:"
        echo "   sudo usermod -aG docker \$USER && newgrp docker"
        exit 1
    fi
fi

# 2. Selección interactiva de carpeta de almacenamiento local
echo "Elige la carpeta de tu dispositivo donde se guardará tu base de datos física (tarjetas.db):"
echo "  - Presiona [ENTER] para usar la carpeta por defecto ('./data' en este directorio)"
echo "  - O escribe una ruta completa (ejemplo: $HOME/Documentos/MisFinanzas)"
echo ""
read -p "📁 Carpeta de datos [./data]: " RUTA_INPUT

if [ -z "$RUTA_INPUT" ]; then
    RUTA_INPUT="./data"
fi

# Expandir tilde o ruta relativa a absoluta
mkdir -p "$RUTA_INPUT"
RUTA_ABS=$(cd "$RUTA_INPUT" && pwd)

echo ""
echo "✅ Carpeta seleccionada: $RUTA_ABS"
echo "📦 Construyendo / Iniciando contenedor de CreditManager..."

# 3. Detener contenedor previo si existe
if $DOCKER_CMD ps -a --format '{{.Names}}' | grep -Eq "^creditmanager$"; then
    echo "🔄 Deteniendo versión previa..."
    $DOCKER_CMD stop creditmanager >/dev/null 2>&1 || true
    $DOCKER_CMD rm creditmanager >/dev/null 2>&1 || true
fi

# 4. Construir o ejecutar imagen
if $DOCKER_CMD images -q creditmanager:latest | grep -q .; then
    echo "🚀 Iniciando imagen existente..."
else
    echo "🔨 Construyendo imagen por primera vez..."
    $DOCKER_CMD build -t creditmanager:latest .
fi

$DOCKER_CMD run -d \
    --name creditmanager \
    --restart unless-stopped \
    -p 3000:3000 \
    -v "$RUTA_ABS:/data" \
    creditmanager:latest >/dev/null

# 5. Obtener IP local para acceso desde celular
IP_LOCAL=$(hostname -I 2>/dev/null | awk '{print $1}' || echo "localhost")

echo ""
echo "=============================================================="
echo "  🎉 ¡CreditManager está funcionando con éxito!"
echo "=============================================================="
echo "  💻 Acceso en este equipo:      http://localhost:3000"
if [ "$IP_LOCAL" != "localhost" ] && [ -n "$IP_LOCAL" ]; then
echo "  📱 Acceso desde tu celular:    http://$IP_LOCAL:3000"
echo "     (conecta tu celular a la misma red Wi-Fi)"
fi
echo "  📁 Base de datos física en:    $RUTA_ABS/tarjetas.db"
echo "=============================================================="
echo ""

# 6. Intentar abrir el navegador automáticamente
if command -v xdg-open &> /dev/null; then
    xdg-open "http://localhost:3000" >/dev/null 2>&1 &
elif command -v open &> /dev/null; then
    open "http://localhost:3000" >/dev/null 2>&1 &
fi

echo "Para detener la aplicación en cualquier momento, ejecuta: ./detener.sh"
echo ""

