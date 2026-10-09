#!/usr/bin/env bash
# ==============================================================
#  CardMaster / CreditManager — Lanzador Global Multiplataforma
# ==============================================================

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=============================================================="
echo "    💳 CardMaster / CreditManager — Menú Principal"
echo "=============================================================="
echo ""
echo "  Elige qué aplicación deseas ejecutar en este dispositivo:"
echo ""
echo "  [1] 🖥️  Linux Ubuntu (Aplicación Nativa 100% de Escritorio)"
echo "  [2] 📱  Android (Servidor Móvil para Celular / PWA / APK)"
echo "  [3] 📦  Instalar acceso directo en Escritorio de Ubuntu"
echo "  [4] 🚪  Salir"
echo ""
read -p "  Selecciona una opción [1-4] (por defecto 1): " OPTION
OPTION=${OPTION:-1}

case "$OPTION" in
    1)
        echo ""
        echo "🚀 Iniciando CardMaster para Linux Ubuntu..."
        exec "$DIR/desktop-ubuntu/iniciar-ubuntu.sh"
        ;;
    2)
        echo ""
        echo "📱 Iniciando CardMaster para Android..."
        exec "$DIR/mobile-android/iniciar-android.sh"
        ;;
    3)
        echo ""
        echo "📦 Instalando acceso directo en Ubuntu..."
        exec "$DIR/desktop-ubuntu/instalar-en-ubuntu.sh"
        ;;
    4)
        echo "Operación cancelada."
        exit 0
        ;;
    *)
        echo "Opción no válida. Iniciando versión de escritorio por defecto..."
        exec "$DIR/desktop-ubuntu/iniciar-ubuntu.sh"
        ;;
esac

