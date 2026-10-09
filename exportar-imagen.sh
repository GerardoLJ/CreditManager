#!/usr/bin/env bash
set -e

echo "📦 Construyendo y empaquetando imagen portable de CreditManager..."
docker build -t creditmanager:latest .

ARCHIVO="creditmanager-image.tar"
echo "💾 Exportando imagen a '$ARCHIVO' (puede tardar un minuto)..."
docker save -o "$ARCHIVO" creditmanager:latest

TAMANO=$(du -h "$ARCHIVO" | cut -f1)
echo ""
echo "=============================================================="
echo "  ✅ Imagen exportada exitosamente:"
echo "     📁 Archivo: $ARCHIVO ($TAMANO)"
echo "=============================================================="
echo "  Puedes subir este archivo a tu Google Drive, Dropbox, etc."
echo "  En cualquier otra computadora (sin USB):"
echo "    1. Descarga '$ARCHIVO'"
echo "    2. Ejecuta: docker load -i $ARCHIVO"
echo "    3. Ejecuta: docker run -d -p 3000:3000 -v /carpeta/que/elijas:/data creditmanager:latest"
echo "=============================================================="

