# ==========================================
# Dockerfile para CreditManager / CardMaster
# Ejecutable en cualquier dispositivo con Docker
# ==========================================

FROM node:20-bookworm-slim

LABEL maintainer="CreditManager"
LABEL description="Control de tarjetas de crédito y finanzas con SQLite persistente"

# Directorio de la aplicación
WORKDIR /app

# Herramientas para dependencias nativas (sqlite3)
RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 \
    make \
    g++ \
    && rm -rf /var/lib/apt/lists/*

# Dependencias
COPY package*.json ./
RUN npm ci --omit=dev || npm install --omit=dev

# Código fuente
COPY . .

# Variables de entorno por defecto
ENV DATA_DIR=/data
ENV PORT=3000
ENV HOST=0.0.0.0
ENV NODE_ENV=production

# Carpeta de datos persistentes
RUN mkdir -p /data

# Volumen para la base de datos física del usuario
VOLUME ["/data"]

EXPOSE 3000

CMD ["node", "server.js"]

