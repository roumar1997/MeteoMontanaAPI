# Cumbre / MeteoMontana — Escalado y preparación para crecer

> Checklist y notas para que la app aguante un pico de usuarios al publicar.
> Basado en la auditoría del backend del 2026-06-24.

## TL;DR — la app NO está hecha para 1 usuario
- Spring MVC atiende peticiones **en paralelo** (Tomcat, ~200 hilos). Las consultas
  NO van en serie.
- Hay **pool de conexiones** (HikariCP), **índices** en todas las tablas calientes,
  y **caché agresiva** del tiempo (Caffeine + caché persistente en Postgres +
  prefetch horario). El coste de Open-Meteo es constante pasen 5 o 5.000 usuarios.
- Lo que se rompe primero en un pico, por orden: **cuotas de Firebase → pool de
  conexiones → plan de Postgres → CPU de la instancia → egress de imágenes**.

## Hecho en código (este commit, en `develop`/staging)
- **Pool** configurable por env `DB_POOL_SIZE` (default subido 5 → 10).
- **AdminStats**: cuenta admins con `COUNT` en BD (antes cargaba TODOS los usuarios
  en memoria → no escalaba).
- **Catálogo de escuelas cacheado** (`@Cacheable("schools-catalog")`), se invalida
  al guardar una escuela. Antes leía las 191 filas en cada petición de lista.
- **Rate-limit por IP** (`RateLimitFilter`, 150 req/min, en memoria) para que un
  cliente con bug/abusivo no agote el pool. (Por instancia; multi-réplica necesita
  Redis.)
- **Métricas** expuestas en `/actuator/metrics` (incluye `hikaricp.connections.active`,
  latencia, errores). `/actuator/health` ya existía.

## Acciones de infraestructura (las haces tú)
1. **Railway → plan Hobby ($5)** es suficiente para miles de usuarios (hasta 48
   vCPU/48 GB por servicio, 5 réplicas). Sube a Pro solo si quieres SLA 99.99% /
   soporte / >5 réplicas / multi-región.
2. **Pool**: pon `DB_POOL_SIZE=15` en las variables de entorno del servicio en
   Railway. Antes confirma el `max_connections` de tu Postgres:
   `SHOW max_connections;` (psql contra la BD de Railway). Regla:
   **(réplicas × pool) ≤ ~70% de max_connections**.
3. **Postgres**: si esperas miles, sube el plan de la BD (más RAM = más conexiones
   y caché).
4. **Firebase (ya en Blaze)**: pon **alertas de presupuesto** en la consola y
   vigila cuotas de Firestore (chat), Storage (fotos) y FCM.
5. **Uptime + alerta**: monitor gratis (UptimeRobot / Better Stack) haciendo ping a
   `https://<tu-backend>/actuator/health` cada 1-5 min → email/Telegram si cae.
6. (Opcional) **Sentry** free para excepciones del backend; Crashlytics ya cubre la
   app.

## Validar ANTES de publicar — prueba de carga
- Script listo en `loadtest/cumbre-loadtest.js` (k6).
- Lánzalo **contra staging**: `k6 run loadtest/cumbre-loadtest.js`.
- Mira `http_req_duration p(95)` y `http_req_failed`. Si al subir usuarios el p95 se
  dispara o salen 5xx/429 → ese es el techo. Sube pool/plan y repite.

## Cuellos de botella conocidos / deuda
- **Rate-limit es por instancia** (en memoria). Con varias réplicas, el límite real
  se multiplica; para un límite compartido haría falta Redis.
- **Catálogo cacheado 90 min**: si una escuela se modifica por un camino que NO pasa
  por `SchoolRepository.save()`, el cambio puede tardar hasta 90 min en verse.
  Verificar que el "mover escuela" del admin pasa por `save()`.
- **Una sola instancia** por defecto: escalar vertical (instancia mayor) antes que
  horizontal; si vas a horizontal, recalcula el pool vs `max_connections`.
- **Egress de imágenes** (Firebase Storage): muchas descargas de fotos = factura
  Blaze. Coil/caché en cliente lo amortigua.
