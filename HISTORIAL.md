# MeteoMontanaAPI — historial de sesiones

> Registro terso por sesión. **No se carga en cada sesión**: se consulta cuando
> hace falta saber por qué algo es como es. Aquí vive lo que no se deduce del
> código: causas raíz, decisiones y trampas. El detalle está en `git log`.
>
> Se retiraron de `CLAUDE.md` (siguen en el historial de git, commit previo a
> 2026-08-05) las secciones de la migración desde la PWA — inventario del
> frontend viejo, alcance del producto, mapa de fases y "qué se queda en el
> frontend" —: la migración terminó y **la PWA está deprecada**, así que
> describían un mundo que ya no existe.


(Las últimas 5-10 sesiones aproximadamente. Las más antiguas se podan.)

### Sesión 2026-07-07 — Escalado para lanzamiento (EN PROD, merge `29b62a3`)

- **Push asíncrono + en lote** (`sendEachForMulticast`, pool `pushExecutor`,
  `PushAsyncConfig`): chat 1-a-1/grupo/quedada, follows y aviso de quedada nueva
  ya no bloquean el hilo de la request → quedadas concurridas escalan a miles.
  `ChatPushController` pasa a depender del puerto `PushSender` (FcmService es
  proxy al llevar `@Async`) — lo cazó `contextLoads`.
- **Subida de fotos** fuera de `@Transactional` (no retiene conexión del pool),
  `multipart.max-file-size=5MB` (arreglado el 1MB oculto), magic bytes
  (`ImageValidation`), cliente de Storage reutilizado.
- Sin migraciones, retrocompatible. Detalle + **ROLLBACK** en `SCALING.md`.
  Pendiente Rodrigo: `DB_POOL_SIZE=25` en Railway prod + alertas Firebase.
- Bug reportado (NO tocado aún): en la app iOS, el mapa de Escuelas no ENCOGE el
  zoom al bajar el radio de distancia — `MapLibreView.swift:311` solo re-encuadra
  con ids NUEVOS (`ids.subtracting(lastFittedIds)`), y un radio menor es subconjunto.

### Sesión 2026-06-23 — Open-Meteo blindado (batch + prefetch) + grupos de chat

- **Open-Meteo 429 por pico** (no caída global como en junio-11): cargar scores
  de ~191 escuelas tras un redeploy → ~191 peticiones de golpe → >600/min → 429
  → cooldown → forecast caído. **Arreglado y en prod**:
  - `OpenMeteoClient`: **batch multi-localización** (lotes de 100 → ~4 calls).
  - **Prefetch horario + caché persistente Postgres**: `ForecastPrefetchScheduler`
    (@Scheduled "0 5 * * * *" + @Async ApplicationReadyEvent) refresca todas las
    escuelas cada hora → tabla `forecast_cache` (V30) + `ForecastStore`.
    `fetchForecast`: memoria → tabla(<6h) → vivo → si 429 sirve dato guardado.
    Uso Open-Meteo ~96/día fijo pasen N usuarios; sobrevive a redeploys.
- **Grupos de chat (backend, solo en `develop`/staging — NO en prod)**:
  `POST /api/chat/group` + `/api/chat/notify-group`; `ChatRepository.createGroup/
  participantsOf`. Reglas Firestore de grupos desplegadas a prod (compartido,
  aditivas; 1-a-1 verificado OK). Para sacar grupos a testers: mergear a `main`
  (prod) + nueva release de la app.
- Recordatorio: backend `develop`→staging, `main`→prod; pedir OK antes de tocar
  prod (ver sección STAGING vs PRODUCCIÓN arriba).

### Sesión 2026-06-13 — secado de roca según viento/sol/temperatura/humedad

- **Mejora del secado en `GetForecastUseCase`**: el secado ya no depende solo
  de tipo de roca + lluvia 72h. Nuevo método `adjustForConditions(baseHours,
  hours)` promedia **viento, nubes, temperatura y humedad** sobre la ventana
  de secado (próximas horas, no el instante actual — de noche engañaría) y
  multiplica un factor por cada uno (más viento/sol/calor/sequedad → seca
  antes), con tope `[0.5, 1.8]` para que ninguna combinación se desmadre.
- **Suelos de seguridad por roca** en `buildDrying`: la arenisca nunca baja de
  **36h** (pierde ~75% de resistencia mojada y su interior sigue empapado
  aunque la superficie seque) y el conglomerado de **18h**, por mucho buen
  tiempo que haga. Basado en guías de escalada (Access Fund, Climbing.com,
  Mountain Hardwear). `buildDrying` ahora recibe la lista de `hours`.
- La base por tipo de roca (granito ~8h, caliza ~12h, conglomerado ~32h,
  arenisca ~48h) y el +50% por lluvia fuerte se mantienen. Sigue siendo
  heurística: usa el proxy de lluvia 72h, no histórico real (TODO sin tocar).
- **Verificado en prod** (Railway): Albarracín (despejado) → 48h, Rozas
  (cubierto + lluvia fuerte) → 73h. Antes daban lo mismo.
- **Lado Android** (repo MeteoMontanaAndroid): el estado de la roca + secado
  sube a una franja con color bajo el índice (`RockStatusBand` en
  `ForecastBody.kt`), antes estaba al final en `BestDayBar`. También: rediseño
  del widget Favoritas a tarjetas y fix de firma del APK de CI.

### Sesión 2026-06-12 (4) — ETag, secado de roca, alerta ventana óptima, fotos en notas

- **ETag/304 en `GET /api/schools`**: `SchoolController` calcula SHA-256 del
  JSON del catálogo y usa `WebRequest.checkNotModified(etag)` → 304 sin body
  si el cliente manda If-None-Match coincidente.
- **Secado de roca en el forecast**: `ForecastResponse.Current` expone
  `drying {wet, dryingHours, message}`. Heurística en `GetForecastUseCase`:
  ~2/3 del lookback del `RockDryingProfile` (caliza 12h, arenisca 48h,
  granito 8h), +50% si la lluvia 72h ≥ 2× el umbral del perfil; aviso
  especial para arenisca aunque el umbral diga "seca".
- **Alerta "ventana óptima hoy"** (V24): columnas `optimal_enabled`,
  `optimal_threshold`, `optimal_last_sent` en `weekend_alert_prefs`.
  `OptimalWindowAlertScheduler` (cron 7-11h Madrid) + `OptimalWindowAlertUseCase`:
  evalúa hasta 6 favoritas del usuario y manda push data-only (targetType
  school) si `bestWindow.avgScore ≥ umbral`; máximo un aviso al día.
  `WeekendAlertController` acepta `optimalEnabled/optimalThreshold`
  (nullables — apps viejas no los pisan) y ya no exige escuelas si la
  alerta de tiempo está desactivada.
- **Fotos en notas** (V23): `photo_url` TEXT en `notes`; `CreateNoteUseCase`
  acepta `photoUrl` opcional (https, ≤1000 chars). La app sube la foto a
  Firebase Storage y aquí solo se guarda la URL.
- Migraciones Flyway hasta **V24**.

### Sesión actual — Materialización de líneas + admin gestión de bloques

**Migración V13** (`db/migration/V13__boulder_fields.sql`): añade tres columnas
TEXT a `pending_contributions` para el flujo BOULDER del front:
- `photo_url` — URL de Firebase Storage de la foto de la piedra propuesta
- `bloques_json` — JSON con la lista de bloques/vías `[{name, grade,
  startType, linePath}]` (lo que el usuario dibujó en la app)
- `topo_lines_json` — reservado (redundante, no usado actualmente)

**Dominio extendido** (`PendingContribution`): nuevos campos
`photoUrl`, `bloquesJson`, `topoLinesJson`. Constructor + getters actualizados.
`PendingContributionJpaEntity` mapea las nuevas columnas con `columnDefinition
= "TEXT"`. `ContributionRequest` y `ContributionResponse` exponen los nuevos
campos. `ContributionResponse` además expone `targetBlockId` (ya estaba en el
dominio) para que el admin en el front distinga propuestas de "añadir vías".

**`ReviewContributionUseCase` — materialización completa de BOULDER al aprobar**:

1. `createBlock(c, type, adminUid)` ahora pasa `c.getPhotoUrl()` como
   `photoPath` al `SchoolBlockJpaEntity` (antes era siempre `null` — error que
   dejaba los bloques aprobados sin foto).
2. **Nuevo `parseAndAttachLines(block, bloquesJson)`**: usa Jackson
   `ObjectMapper` para parsear el JSON enviado por el front; crea un
   `BlockLineJpaEntity` por cada bloque con UUID nuevo, nombre, grado,
   `startType` mapeado y `linePath` (string JSON de puntos normalizados);
   lo añade con `block.addLine(line)` y guarda el `SchoolBlockJpaEntity` —
   el cascade `@OneToMany(cascade = ALL)` persiste las filas en `block_lines`.
3. **Nuevo `addLinesToExistingBlock(c)`**: cuando la contribución BOULDER
   tiene `targetBlockId != null` (caso "+ AÑADIR VÍAS" desde el front),
   busca el bloque existente, calcula el siguiente `sortOrder` continuando
   desde las líneas existentes, y añade las nuevas. NO crea piedra nueva.
4. **Mapeo de `startType`**: `mapStartType()` convierte los valores que
   envía la app (`PIE/SIT/LANCE/TRAV`) a los del enum del backend
   (`STAND/SIT/JUMP/TRAV`). El front al editar un block del backend hace la
   conversión inversa.

**Admin puede editar/borrar bloques de cualquier usuario** — `SchoolBlockUseCase`:

- `delete(requesterUid, blockId)`: antes requería `b.getCreatedByUid().equals(adminUid)`
  (solo el creador). Ahora también permite admins consultando
  `userRepository.findByUid(uid).map(u -> u.isAdmin())`. Sin esto el admin no
  podía gestionar bloques propuestos por usuarios. Las líneas se borran en
  cascada por el FK `block_lines.block_id ON DELETE CASCADE`.
- `update(editorUid, blockId, req)`: mismo cambio — creador OR admin.
  Permite que el panel admin del front cambie nombre, coords, descripción y
  líneas de cualquier bloque via `PUT /api/blocks/{blockId}`.
- Constructor de `SchoolBlockUseCase` ahora recibe también `UserRepository`
  (inyectado por Spring) — sin acoplarse a `AdminGuard`, solo lee el flag.

**Compatibilidad**: las migraciones V13 son aditivas (`ADD COLUMN ... TEXT`),
no afectan a propuestas ni bloques antiguos. Pero los bloques aprobados
**antes** de los fixes de `parseAndAttachLines` y `c.getPhotoUrl()` están en
BD sin foto ni líneas en `block_lines`. Si hace falta limpiar, se pueden
borrar desde el panel admin del front (tab GESTIONAR).

- **Fase 1 completa**: filtros region/style/rock(lista)/distance funcionando
  en `GET /api/escuelas`. `GeoDistance` con Haversine creado.
- Decisión de stack: Postgres + JPA + Spring Security; Firebase Auth y FCM
  como servicios externos. Chat se queda en Firestore por realtime.
- Auditoría completa del front tras `git pull origin main` (311 commits de
  retraso). Inventario real reflejado en sección "Inventario real".
- Lección apuntada: ejecutar `git fetch && git status -sb` ANTES de cualquier
  conclusión sobre el front. "Clean" no = al día con remoto.
- **Fase 2.1 ✅**: Postgres 16 corriendo en Docker (`docker-compose.yml` en
  raíz). `.env` con `POSTGRES_PASSWORD`. `.env.example` committeable.
  `.gitignore` actualizado para excluir `.env*` salvo `.env.example`.
- **Fase 2.2 ✅**: deps JPA + driver Postgres + Flyway añadidos a `pom.xml`.
  `application.yaml` configurado con datasource, JPA (`ddl-auto: validate`,
  `show-sql: true`), y `spring.config.import: optional:file:../.env[.properties]`
  para reusar la contraseña del docker-compose.
- **Fase 2 ✅ COMPLETA**: Postgres en Docker, JPA con Flyway, seed de 191
  escuelas desde JSON, arquitectura hexagonal con `JpaSchoolRepositoryAdapter`,
  rename completo a inglés (`School`, `/api/schools`). JSON y clases en español
  eliminados. `School.java` limpiado de `@JsonProperty` — POJO puro sin
  dependencias de framework. Endpoints `/api/schools` y `/api/schools/{id}`
  funcionando con filtros (region, style, rockType, distancia).
- **Fase 11 ✅ COMPLETA**: Dockerfile multi-stage (JDK 21 build + JRE 21
  runtime, usuario no-root), `.dockerignore`, GitHub Actions CI con servicio
  Postgres en `.github/workflows/ci.yml` (corre tests + build), Spring
  Actuator añadido para `/actuator/health` (público en `SecurityConfig`),
  `application.yaml` parametrizado con env vars (`DATABASE_URL`,
  `DATABASE_USERNAME`, `PORT`, `JPA_SHOW_SQL`). `DEPLOY.md` con guía
  para Railway/Render/Fly.io.
- **Fase 10 ✅ COMPLETA**: FCM push. `FcmService` envuelve FirebaseMessaging.
  Columna `fcm_token` en users (Flyway V6) + `PUT /api/me/fcm-token`.
  `@EnableScheduling` + `@EnableAsync` en `ApiApplication`.
  `SubmissionCleanupScheduler` cron 03:00 borra submissions revisadas >5 días.
  `SubmissionReviewedEvent` + listener `@Async` manda push al autor al
  aprobar/rechazar. `SendAdminPushUseCase` + `POST /api/admin/push`.
- **Fase 9 ✅ COMPLETA**: tablas `school_submissions` y `admin_logs`
  (Flyway V5). `AdminGuard` valida `is_admin=true` en BD.
  `ApproveSubmissionUseCase` crea School + actualiza submission + log
  todo en `@Transactional`. Endpoints `POST /api/submissions`,
  `GET /api/submissions/me`, `GET /api/admin/submissions`,
  `POST /api/admin/submissions/{id}/approve|reject`, `GET /api/admin/logs`.
- **Fase 8 ✅ COMPLETA**: tabla `users` (Flyway V4). `User` dominio,
  `UserJpaEntity`, `UserRepository`, adaptador. DTOs: `PublicProfileDto`
  (sin email), `PrivateProfileDto` (con email, flags), `UpdateProfileRequest`.
  Use cases: `GetOrCreateMyProfileUseCase` (auto-crea en primer login —
  JIT provisioning), `GetPublicProfileUseCase` (busca por uid o username,
  oculta perfiles privados con 404), `UpdateMyProfileUseCase` (valida
  username regex `[a-z0-9_]{3,20}`, detecta colisión → 409). Endpoints:
  `GET /api/me`, `PUT /api/me`, `GET /api/users/{identifier}` (público).
  `UserDtoMapper` resuelve `photoPath` → URL firmada.
- **Fase 7 ✅ COMPLETA**: tabla `school_photos` (Flyway V3) con FK a `schools`.
  `StorageService` envuelve Firebase Storage (upload, delete, signedReadUrl
  v4). `UploadSchoolPhotoUseCase` valida content-type image/*, max 5MB,
  transaccional con rollback de Storage si Postgres falla. `GetSchoolPhotosUseCase`
  genera URLs firmadas a 60min. `DeleteSchoolPhotoUseCase` solo el uploader.
  Endpoints: `GET /api/schools/{id}/photos` (público), `POST` (multipart, auth),
  `DELETE /api/photos/{photoId}` (auth + ownership). `FirebaseConfig` configura
  bucket `climbingteams.firebasestorage.app`.
- **Fase 6 ✅ COMPLETA**: endpoint `GET /api/schools/{id}/forecast`.
  `OpenMeteoClient` con `RestClient` llama a Open-Meteo. `OpenMeteoResponse`
  DTO mapea el JSON con `@JsonProperty`. `GetForecastUseCase` combina escuela
  + tiempo + score por hora (usa `ClimbScoreCalculator` y `RockDryingProfile`).
  `ForecastResponse` DTO de salida. `@EnableCaching` + `@Cacheable` cachean
  por lat,lon — primera llamada ~500ms, siguientes ~10ms. TODO: TTL con
  Caffeine (los datos de Open-Meteo no expiran solos).
- **Fase 5 ✅ COMPLETA**: `ClimbScoreCalculator` y `RockDryingProfile` en
  `domain/score/` — traducción exacta de `score-core.js`. 9 tests JUnit en
  `src/test/` verifican caps de lluvia, perfiles de roca, labels y bounds.
- **Fase 4 ✅ COMPLETA**: Spring Security añadido. `FirebaseTokenFilter`
  valida JWT de Firebase en cada request. `SecurityConfig` define endpoints
  públicos (GET /api/schools/**) y protegidos (todo lo demás). CORS configurado
  para PWA local y producción. `FirebaseUser` record como principal del contexto.
- **Fase 3 ✅ COMPLETA**: tabla `notes` con FK a `schools` (Flyway V2),
  `NoteJpaEntity`, `Note` (dominio), `NoteRepository` (puerto),
  `SpringDataNoteRepository` (query derivada), `JpaNoteRepositoryAdapter`,
  `GetNotesBySchoolUseCase`, endpoint `GET /api/schools/{id}/notes`.
  Script de migración en `tools/migrate-notes/` — lee Firestore con
  `collectionGroup("notas")`, hace match escuela por coordenadas (Haversine SQL),
  inserta en Postgres. 4 notas migradas correctamente. Postman: `200 OK`.

