#!/usr/bin/env bash

# Directorio de este proyecto
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

# Ejecutar como aplicación nativa 100% de escritorio
python3 "$DIR/desktop-app.py"
