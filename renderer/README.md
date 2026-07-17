# cumbre-renderer

Mini-servicio que convierte las **historias HTML de Cumbre** en imágenes PNG
1080×1920, para el proyecto de Instagram automático (n8n). Está **aislado** del
backend de producción: usa su propio Chrome headless, así que si se satura
generando imágenes, la app de los usuarios ni se entera.

## Endpoints

- `GET /health` → `ok`
- `GET /render?url=<URL>&w=1080&h=1920` → PNG
  - `url` debe ser una historia de Cumbre (`api.climbingteams.com` o el staging;
    whitelist en `server.js`).
  - `w`/`h` opcionales (por defecto 1080×1920).

Ejemplo:
```
https://<renderer>/render?url=https%3A%2F%2Fapi.climbingteams.com%2Fapi%2Fsocial%2Fconditions%2Fhtml%3Fregion%3DComunidad%2520de%2520Madrid
```

## Desplegar en Railway (nuevo servicio del proyecto zoological-wisdom)

1. Railway → proyecto `zoological-wisdom` → **New → GitHub Repo** →
   `roumar1997/MeteoMontanaAPI`.
2. En **Settings** del servicio nuevo:
   - **Root Directory**: `renderer`
   - **Builder**: Dockerfile (lo detecta solo por el `Dockerfile`).
3. Deploy. Railway inyecta `PORT`; el server ya lo respeta.
4. Genera un dominio público (Settings → Networking → Generate Domain) y prueba
   `https://<dominio>/health` → `ok`.

No necesita variables de entorno (el Chromium del sistema está en
`/usr/bin/chromium`, ya configurado en el Dockerfile).

## Local (para probar en Windows)

```
npm install
set PUPPETEER_EXECUTABLE_PATH=C:\Program Files\Google\Chrome\Application\chrome.exe
set PORT=3999
node server.js
```
