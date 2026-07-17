// Cumbre — mini-servicio de render HTML → PNG (proyecto n8n / Instagram).
// Aislado del backend de producción: si se satura generando imágenes, la app
// de los usuarios ni se entera. n8n le pide la imagen ya hecha y la publica.
//
//   GET /render?url=<historia>&w=1080&h=1920  →  PNG
//   GET /health                               →  "ok"
//
// Solo renderiza URLs de nuestros propios dominios (whitelist).

const express = require('express');
const puppeteer = require('puppeteer-core');

const app = express();
const PORT = process.env.PORT || 3000;
const CHROME = process.env.PUPPETEER_EXECUTABLE_PATH || '/usr/bin/chromium';

// Solo dejamos renderizar historias de Cumbre (prod y staging).
const ALLOWED = /^https:\/\/(api\.climbingteams\.com|meteomontanaapi-staging\.up\.railway\.app)\//;

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

  if (!url) return res.status(400).type('text').send('falta ?url');
  if (!ALLOWED.test(url)) return res.status(403).type('text').send('url no permitida');

  let page;
  try {
    const browser = await getBrowser();
    page = await browser.newPage();
    await page.setViewport({ width, height, deviceScaleFactor: 1 });
    await page.goto(url, { waitUntil: 'networkidle0', timeout: 30000 });
    // Espera a que las fuentes (Google Fonts) estén cargadas antes de capturar.
    await page.evaluate(() => document.fonts && document.fonts.ready);
    const png = await page.screenshot({
      type: 'png',
      clip: { x: 0, y: 0, width, height },
    });
    res.set('Content-Type', 'image/png');
    res.set('Cache-Control', 'no-store');
    // Puppeteer moderno devuelve Uint8Array; Express serializaría eso como JSON
    // → lo pasamos a Buffer para que salga como binario PNG de verdad.
    res.send(Buffer.from(png));
  } catch (err) {
    console.error('render error:', err.message);
    res.status(500).type('text').send('render error: ' + err.message);
  } finally {
    if (page) { try { await page.close(); } catch (_) {} }
  }
});

app.listen(PORT, () => console.log('cumbre-renderer escuchando en :' + PORT));
