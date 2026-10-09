#!/usr/bin/env bash
echo "Deteniendo CardMaster en puerto 3000..."
fuser -k 3000/tcp 2>/dev/null || true
echo "✅ CardMaster detenido."

