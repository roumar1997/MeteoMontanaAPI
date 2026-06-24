// Prueba de carga de Cumbre / MeteoMontana con k6 (https://k6.io).
//
// QUÉ HACE: simula muchos usuarios "navegando" (los endpoints públicos pesados:
// catálogo de escuelas, scores de hoy, detalle y forecast de una escuela). Sube
// de 0 a 200 usuarios virtuales y mide latencia (p95) y % de errores. Donde
// empiecen a fallar/lentificarse → ese es el techo actual.
//
// CÓMO LANZARLO (contra STAGING, nunca prod):
//   1) Instala k6:  https://k6.io/docs/get-started/installation/
//      (Windows: `winget install k6` o `choco install k6`)
//   2) Ejecuta:
//        k6 run loadtest/cumbre-loadtest.js
//      o apuntando a otra URL:
//        k6 run -e BASE_URL=https://meteomontanaapi-staging.up.railway.app loadtest/cumbre-loadtest.js
//
// LEER EL RESULTADO: mira `http_req_duration p(95)` y `http_req_failed`.
//   - p(95) < 800ms y errores < 1%  → vas sobrado.
//   - Si al subir de usuarios el p(95) se dispara o aparecen 5xx/429 → ahí está
//     el límite. Sube DB_POOL_SIZE / el plan de Railway y vuelve a medir.
//
// NOTA: solo prueba endpoints PÚBLICOS (lectura), que es lo que martillea un
// pico de gente nueva navegando. Para endpoints autenticados habría que pasar un
// token Firebase válido en la cabecera Authorization (ver comentario abajo).

import http from 'k6/http';
import { check, sleep, group } from 'k6';

const BASE = __ENV.BASE_URL || 'https://meteomontanaapi-staging.up.railway.app';

export const options = {
  stages: [
    { duration: '1m', target: 50 },   // calentamiento: subir a 50 usuarios
    { duration: '2m', target: 50 },   // sostener 50
    { duration: '1m', target: 200 },  // pico: subir a 200
    { duration: '2m', target: 200 },  // sostener 200
    { duration: '1m', target: 0 },    // enfriar
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],            // < 1% de errores
    http_req_duration: ['p(95)<800'],          // p95 < 800 ms
  },
};

export default function () {
  group('catálogo', () => {
    const r = http.get(`${BASE}/api/schools`);
    check(r, { 'schools 200': (res) => res.status === 200 });
  });

  group('scores de hoy', () => {
    // Ajusta unos ids reales de tu catálogo si quieres; estos son de ejemplo.
    const ids = '1,2,3,4,5,6,7,8,9,10';
    const r = http.get(`${BASE}/api/forecast/today-scores?ids=${ids}`);
    check(r, { 'scores 200': (res) => res.status === 200 });
  });

  group('detalle + forecast', () => {
    const id = '1'; // pon un id de escuela real de tu catálogo
    const d = http.get(`${BASE}/api/schools/${id}`);
    check(d, { 'detalle 200': (res) => res.status === 200 || res.status === 404 });
    const f = http.get(`${BASE}/api/schools/${id}/forecast`);
    check(f, { 'forecast ok': (res) => res.status === 200 || res.status === 404 });
  });

  // Endpoints AUTENTICADOS (descomentar y poner un idToken Firebase de prueba):
  // const TOKEN = __ENV.FB_TOKEN;
  // const auth = { headers: { Authorization: `Bearer ${TOKEN}` } };
  // const me = http.get(`${BASE}/api/me`, auth);
  // check(me, { 'me 200': (res) => res.status === 200 });

  sleep(1); // cada usuario espera ~1s entre rondas (más realista)
}
