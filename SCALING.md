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
- **Rate-limit por IP** (`RateLimitFilter`, en memoria) para que un cliente con
  bug/abusivo no agote el pool. Configurable con env **`RATE_LIMIT_PER_MINUTE`**
  (default **600**/min/IP; `0` lo desactiva). 600 es generoso para no estorbar a
  usuarios reales ni a varios detrás del mismo NAT móvil, pero corta un flood.
  (Por instancia; multi-réplica necesita Redis.)

> **Para medir la capacidad real con k6**: como la prueba sale de UNA IP, el
> rate-limit la trata como un cliente abusivo y devuelve 429 a casi todo (el
> resultado sale 99% "fallido" aunque el server vaya fino). Para medir de verdad:
> pon `RATE_LIMIT_PER_MINUTE=0` en staging, lanza k6, y vuelve a quitarlo. Sin eso,
> el dato útil es la latencia de las peticiones que SÍ pasan (p95 ~42 ms = rápido).
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

---

## Cambios de escalado del 2026-07-07 (EN PRODUCCIÓN, commit merge `29b62a3`)

Backend-only, retrocompatible, **sin migraciones**. Las apps ya instaladas se
benefician sin actualizar. Verificado: `mvnw test` → 31 tests OK (incl.
`contextLoads` con Postgres real) + `mvnw -DskipTests package` → BUILD SUCCESS.

**Qué se tocó y por qué:**
1. **Push asíncrono + en lote** — `FcmService` gana `sendDataToUserAsync` /
   `sendDataToUsersAsync` (en el puerto `PushSender`). Envían con
   `sendEachForMulticast` (1 llamada a FCM por tanda de 500 tokens en vez de una
   por dispositivo) en un pool dedicado `pushExecutor` (`PushAsyncConfig`).
   Usado por chat 1-a-1, chat de grupo/quedada (`ChatPushController`), follows
   (`FollowUseCase`) y aviso de quedada nueva (`CreateMeetupUseCase`). Antes el
   fan-out era síncrono y bloqueaba el hilo de la request → cuello en quedadas.
   - Efecto secundario obligado: `ChatPushController` pasa a depender del puerto
     `PushSender` (no la clase `FcmService`), porque al llevar `@Async` Spring la
     expone como proxy de la interfaz.
2. **Subida de fotos sin retener conexión** — `UploadSchoolPhotoUseCase` y
   `UpdateProfilePhotoUseCase` dejan de ser `@Transactional` (la subida a
   Storage tarda segundos y mantenía ocupada una conexión del pool de 10).
3. **`application.yaml`**: `spring.servlet.multipart.max-file-size=5MB`
   (el default oculto de 1MB rechazaba fotos a resolución completa).
4. **Validación de magic bytes** (`ImageValidation`) además del Content-Type.
5. **`StorageService`** reutiliza el cliente de Storage (antes creaba uno por foto).

**Acción pendiente de Rodrigo (no aplicada por código):**
- Poner `DB_POOL_SIZE=25` en las variables del servicio de PRODUCCIÓN en Railway
  (tras confirmar `SHOW max_connections;`). Es lo que más sube el techo real.
- Alertas de presupuesto en Firebase (Storage/FCM) por el mayor volumen de push.

**ROLLBACK si algo falla en prod:**
```
# Revierte SOLO este cambio, deja el resto de main intacto:
git checkout main && git revert -m 1 29b62a3 && git push origin main
# Railway redespliega la versión anterior automáticamente.
```
Como no hubo migraciones ni cambios de esquema, revertir es seguro y no deja
datos a medias. Señales de que habría que revertir: los push dejan de llegar,
`/actuator/health` no da UP, o subir fotos empieza a fallar.
