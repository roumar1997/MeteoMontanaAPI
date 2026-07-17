# Mini-servicio de render HTML→PNG de Cumbre. Chromium del sistema + puppeteer-core
# (no descarga su propio Chromium → imagen más ligera y build fiable en Railway).
FROM node:20-slim

# Chromium + fuentes (Liberation para sans/serif genéricas, Noto para emojis
# y acentos). Sin recomendados para no inflar la imagen.
RUN apt-get update && apt-get install -y --no-install-recommends \
      chromium \
      fonts-liberation \
      fonts-dejavu-core \
      fonts-noto-core \
      fonts-noto-color-emoji \
      ca-certificates \
    && rm -rf /var/lib/apt/lists/*

ENV PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium
ENV NODE_ENV=production

WORKDIR /app
COPY package*.json ./
RUN npm install --omit=dev
COPY . .

# Railway inyecta PORT; el server ya lo respeta.
CMD ["node", "server.js"]
