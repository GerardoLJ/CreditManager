#!/usr/bin/env bash
echo "Deteniendo CardMaster en macOS..."
lsof -ti:3000 | xargs kill -9 2>/dev/null || true
echo "✅ CardMaster detenido."

