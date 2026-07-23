// Cumbre — mini-servicio de render HTML → PNG (proyecto n8n / Instagram).
// Aislado del backend de producción: si se satura generando imágenes, la app
// de los usuarios ni se entera. n8n le pide la imagen ya hecha y la publica.
//
//   GET /render?url=<historia>&w=1080&h=1920  →  PNG
//   GET /health                               →  "ok"
//
// Solo renderiza URLs de nuestros propios dominios (whitelist).
//
// CACHÉ (2026-07-23): Instagram descarga la image_url con un timeout de pocos
// segundos, pero Railway DUERME este servicio y despertarlo + arrancar Chromium
// tarda ~100 s → Instagram se rendía con el error 2207052. Solución: guardamos
// el PNG ya renderizado en memoria un rato. El flujo n8n calienta el renderer
// (1ª petición, lenta) ANTES de publicar; cuando Instagram descarga, la imagen
// ya está cacheada = instantánea. Ver INSTAGRAM_AUTOMATION.md.

const express = require('express');
const puppeteer = require('puppeteer-core');

const app = express();
const PORT = process.env.PORT || 3000;
const CHROME = process.env.PUPPETEER_EXECUTABLE_PATH || '/usr/bin/chromium';
// Cuánto vive un PNG en caché. Un run diario renderiza fresco (ayer ya expiró);
// dentro del mismo run, calentar + publicar ocurren en segundos → acierto.
const CACHE_TTL_MS = parseInt(process.env.CACHE_TTL_MS, 10) || 15 * 60 * 1000;
const CACHE_MAX = 100;

// Solo dejamos renderizar historias de Cumbre (prod y staging).
const ALLOWED = /^https:\/\/(api\.climbingteams\.com|meteomontanaapi-staging\.up\.railway\.app)\//;

// Caché en memoria: clave (url|WxH) → { png: Buffer, expires: epochMs }.
const cache = new Map();

function cacheGet(key) {
  const entry = cache.get(key);
  if (!entry) return null;
  if (Date.now() > entry.expires) { cache.delete(key); return null; }
  return entry.png;
}

function cacheSet(key, png) {
  cache.set(key, { png, expires: Date.now() + CACHE_TTL_MS });
  // Poda simple FIFO para no crecer sin límite.
  if (cache.size > CACHE_MAX) cache.delete(cache.keys().next().value);
}

let browserPromise = null;
async function getBrowser() {
  if (!browserPromise) {
    browserPromise = puppeteer.launch({
      executablePath: CHROME,
      headless: 'new',
      args: [
        '--no-sandbox',
        '--disable-setuid-sandbox',
        '--disable-dev-shm-usage',        // evita crashes por /dev/shm pequeño en contenedores
        '--font-render-hinting=none',
      ],
    }).catch((e) => { browserPromise = null; throw e; });
  }
  return browserPromise;
}

app.get('/health', (_req, res) => res.type('text').send('ok'));

app.get('/render', async (req, res) => {
  const url = req.query.url;
  const width = Math.min(parseInt(req.query.w, 10) || 1080, 2160);
  const height = Math.min(parseInt(req.query.h, 10) || 1920, 3840);
  const noCache = req.query.nocache === '1';

  if (!url) return res.status(400).type('text').send('falta ?url');
  if (!ALLOWED.test(url)) return res.status(403).type('text').send('url no permitida');

  const cacheKey = `${url}|${width}x${height}`;

  // Acierto de caché → responde al instante (esto es lo que ve Instagram).
  if (!noCache) {
    const hit = cacheGet(cacheKey);
    if (hit) {
      res.set('Content-Type', 'image/png');
      res.set('Cache-Control', 'public, max-age=300');
      res.set('X-Cache', 'HIT');
      return res.send(hit);
    }
  }

  let page;
  try {
    const browser = await getBrowser();
    page = await browser.newPage();
    await page.setViewport({ width, height, deviceScaleFactor: 1 });
    await page.goto(url, { waitUntil: 'networkidle0', timeout: 30000 });
    // Espera a que las fuentes (Google Fonts) estén cargadas antes de capturar.
    await page.evaluate(() => document.fonts && document.fonts.ready);
    const png = Buffer.from(await page.screenshot({
      type: 'png',
      clip: { x: 0, y: 0, width, height },
    }));
    cacheSet(cacheKey, png);
    res.set('Content-Type', 'image/png');
    res.set('Cache-Control', 'public, max-age=300');
    res.set('X-Cache', 'MISS');
    // Puppeteer moderno devuelve Uint8Array; lo pasamos a Buffer (arriba) para
    // que Express lo mande como binario PNG de verdad y no como JSON.
    res.send(png);
  } catch (err) {
    console.error('render error:', err.message);
    res.status(500).type('text').send('render error: ' + err.message);
  } finally {
    if (page) { try { await page.close(); } catch (_) {} }
  }
});

app.listen(PORT, () => console.log('cumbre-renderer escuchando en :' + PORT));
