#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=============================================================="
echo "    📦 Instalador de Acceso Directo de CardMaster en Ubuntu"
echo "=============================================================="
echo ""

# 1. Dar permisos de ejecución
chmod +x "$DIR/iniciar-ubuntu.sh"
chmod +x "$DIR/detener-ubuntu.sh"
chmod +x "$DIR/CardMaster.desktop"

# 2. Configurar lanzador con rutas absolutas dinámicas
DESKTOP_FILE="$DIR/CardMaster.desktop"
sed -i "s|^Exec=.*iniciar-ubuntu.sh|Exec=$DIR/iniciar-ubuntu.sh|g" "$DESKTOP_FILE"
sed -i "s|^Icon=.*|Icon=$DIR/public/icon-512.png|g" "$DESKTOP_FILE"
sed -i "s|Exec=.*detener-ubuntu.sh|Exec=$DIR/detener-ubuntu.sh|g" "$DESKTOP_FILE"

# 3. Instalar en el menú de aplicaciones de Ubuntu (~/.local/share/applications)
APPS_DIR="$HOME/.local/share/applications"
mkdir -p "$APPS_DIR"
cp "$DESKTOP_FILE" "$APPS_DIR/CardMaster.desktop"
chmod +x "$APPS_DIR/CardMaster.desktop"

# 4. Instalar en el Escritorio del usuario si existe
DESKTOP_DIR=""
if [ -d "$HOME/Escritorio" ]; then
    DESKTOP_DIR="$HOME/Escritorio"
elif [ -d "$HOME/Desktop" ]; then
    DESKTOP_DIR="$HOME/Desktop"
fi

if [ -n "$DESKTOP_DIR" ]; then
    cp "$DESKTOP_FILE" "$DESKTOP_DIR/CardMaster.desktop"
    chmod +x "$DESKTOP_DIR/CardMaster.desktop"
    # Habilitar ejecución directa sin advertencias en GNOME Ubuntu
    gio set "$DESKTOP_DIR/CardMaster.desktop" metadata::trusted true 2>/dev/null || true
    echo "✅ Icono creado en tu Escritorio: $DESKTOP_DIR/CardMaster.desktop"
fi

echo "✅ CardMaster agregado a tu menú de aplicaciones de Ubuntu."
echo ""
echo "🎉 ¡LISTO! Ahora puedes:"
echo "   1. Hacer DOBLE CLIC en el icono de CardMaster en tu Escritorio."
echo "   2. O buscar 'CardMaster' en tus aplicaciones de Ubuntu."
echo "   3. (Opcional) Hacer clic derecho en el icono en la barra lateral y elegir 'Añadir a favoritos'."
echo "=============================================================="

