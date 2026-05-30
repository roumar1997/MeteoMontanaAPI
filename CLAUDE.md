# MeteoMontanaAPI — contexto para Claude

Backend Spring Boot (Java) que está absorbiendo el frontend MeteoMontana
(`C:\Users\rouma\Desktop\MeteoMontana`). Migración por fases, sin big bang —
strangler fig: cada endpoint nuevo convive con el código viejo hasta que el
frontend deja de usarlo.

## Workflow de cada sesión (importante)

**Primer mensaje de una sesión nueva**: el usuario dirá *"léete CLAUDE.md y
sigamos por donde lo dejamos"*. Tú lees este archivo, miras la sección
**Estado actual** al final, y arrancas desde ahí. **No** repitas el plan
entero; ve directo al siguiente sub-paso.

**Antes de cerrar la sesión**: actualiza **Estado actual** y la sección
**Bitácora reciente** con un resumen corto (3-5 líneas) de qué se ha hecho.
Marca con `[x]` lo que esté terminado en el mapa de fases.

## Cómo trabaja el usuario (contrato pedagógico)

El usuario es desarrollador junior. Quiere convertirse en backend Java
competente. **No quiere atajos**: quiere entender cada línea que escribe y por
qué se hace así.

Reglas que sigues SIEMPRE:

1. **Idioma de comunicación: español.** Idioma del código: **inglés**
   (entidades, métodos, URLs, comentarios). Excepciones razonadas si las hay.

2. **Paso a paso.** Una decisión / un cambio / una clase a la vez. Esperas a
   que el usuario lo aplique y pruebe antes del siguiente.

3. **El usuario escribe el código.** Tú das los snippets explicados; no
   editas archivos `.java` salvo que te lo pida explícitamente. Sí puedes
   editar este `CLAUDE.md` y archivos de configuración (`pom.xml`,
   `docker-compose.yml`, etc.) cuando el caso lo justifique.

4. **Formato de explicación de código nuevo:**
   - **Contexto** (qué vamos a hacer y por qué la app lo necesita).
   - **Código completo** del bloque/archivo, no fragmentos sueltos.
   - **Bloque a bloque o línea a línea**: qué hace, por qué está, qué pasaría
     si lo quitaras.
   - **Conceptos nuevos enmarcados** (anotaciones, patrones, librerías) con
     mini-explicación reusable.
   - **Pregunta de senior al final**: un trade-off, un caso límite, una
     decisión alternativa, para que el usuario piense.

5. **Si introduces un concepto nuevo**, asume cero o poco conocimiento previo
   y explícalo. El usuario tiene huecos básicos en SQL y nunca ha usado
   Docker (a fecha de inicio Fase 2). Spring Boot, JPA, JUnit son terreno
   nuevo cuando aparezcan.

6. **Verifica antes de proponer.** Si el cambio toca código existente, léelo
   antes con Read/Grep. No asumas.

7. **Trade-offs explícitos** cuando haya decisión de diseño. Da tu
   recomendación y el porqué, pero el usuario decide.

8. **No te avergüences de mandarle parar y preguntar** si crees que algo no
   está claro. Y respeta cuando él te pare.

## Decisión de stack (tomada conscientemente)

**Postgres + Spring Data JPA + Spring Security**, con **Firebase Auth y FCM
como servicios externos** que se mantienen.

Por qué: maximizar aprendizaje del stack canónico Java enterprise y
empleabilidad. Firestore desde el back era cómodo pero nicho. Auth/Push se
quedan en Firebase porque migrarlos no aporta valor y rompería la experiencia
de usuarios existentes.

Resumen:

- **Datos de negocio** (schools, notes, journal, contributions, follows,
  votes, submissions) → **Postgres**.
- **Login** → Firebase Auth, validado en el back como JWT con Spring Security.
- **Push** → FCM, integrado desde Spring vía Admin SDK.
- **Chats entre usuarios** → **se quedan en Firestore**. Realtime es esencial
  para chat; replicarlo en Postgres no compensa. Excepción consciente.
- **Cloud Functions actuales** → se evalúan caso a caso: algunas pasan a
  `@Scheduled` en Spring, otras se quedan.

## Renombrado a inglés (en curso en Fase 2)

Las clases nuevas que aparezcan en Fase 2+ se nombran en inglés. Las viejas
de Fase 1 se renombran **cuando se reemplacen** (no big-bang). Convenciones:

| Concepto | Nombre inglés |
|---|---|
| Escuela | `School` |
| ccaa | `region` |
| estilo | `style` |
| roca | `rockType` |
| ubicacion | `location` |
| fuente | `source` |
| lat / lon | `lat` / `lon` (sin cambio, convención internacional) |
| Nota | `Note` |
| sector_submissions | `school_submissions` |
| sectores_community | `community_schools` |

URLs públicas también pasan a inglés: `/api/schools`, `/api/schools/{id}/notes`,
etc. Frontend tendrá que adaptarse — lo gestionamos en la fase de cliente.

## Arquitectura

Hexagonal dentro de `api/src/main/java/com/meteomontana/api/`:

- `domain/model/` — entidades puras (`Escuela`/`School`, …)
- `domain/port/` — interfaces de repositorio
- `domain/exception/` — excepciones de dominio
- `domain/util/` — utilidades puras del dominio (p. ej. `GeoDistance`)
- `application/` — casos de uso (`GetSchoolsUseCase`, …)
- `infrastructure/web/` — controllers REST
- `infrastructure/persistence/` — implementaciones de repositorios (JSON,
  Firestore, JPA)
- `infrastructure/security/` — filtros de Firebase Auth (a partir de Fase 4)

Arrancar la app: `./mvnw spring-boot:run` desde `api/`. Base URL:
`http://localhost:8080/api`.

## Inventario real del frontend actual (auditoría tras git pull)

Resultado de leer `C:\Users\rouma\Desktop\MeteoMontana` archivo por archivo
**con la versión actualizada de `origin/main`** (la versión vieja que
auditamos antes estaba 311 commits por detrás). La app es **mucho más
completa** de lo que parecía. La mayoría de cosas que pensaba "construir"
ya existen.

### Firebase project

- **Project ID**: `climbingteams` (Firebase).
- **Auth providers activos**: solo Google. No email/password.
- **Web app config** en `js/firebase/core.js`.
- **Storage** activo (fotos).

### Colecciones Firestore reales

**`users/{uid}`** — extendido:
```
{
  // identidad pública
  username: string (único, lowercase, 3-20 [a-z0-9_]),
  displayName, photoURL, photoPath,
  bio: string (≤150 chars),
  isPublic: boolean,            // perfil público vs privado
  topGrade: '7c' | …,           // grado máximo para perfil
  followersCount, followingCount,  // denormalizados via CF
  // privacidad / admin
  isAdmin?: boolean,
  isPremium?: boolean,
  // datos de uso
  favorites: [{ name, lat, lon }],
  fcmToken, lastLat, lastLon,
  notificationPrefs: { enabled, hour, mode, maxDistance, rockTypes[],
                        favoriteSectors[], selectedDays[] }
}
```

Subcolecciones de `users/{uid}`:
- `followers/{followerId}` — `{ followedAt }`
- `following/{targetUid}` — `{ followedAt }`
- `follow_requests/{requesterId}` — `{ requestedAt }` (perfiles privados)
- `notifications/{notifId}` — bandeja de notificaciones (solo CFs escriben)

**`usernames/{username}`** — `{ uid }` — garantiza unicidad de username.

**`conversations/{convId}`** — chat directo. `convId = sortedUids.join('_')`.
```
{
  participants: [uid1, uid2],
  lastMessage, lastFromUid, lastAt,
  unread_{uid1}: number, unread_{uid2}: number
}
```
Subcolección `messages/{msgId}`: `{ fromUid, text (≤1000), createdAt }`.

**`journal/{uid}/sessions/{sessionId}`** — diario personal de escalada:
```
{ date, escuelaName, escuelaId, sectorName, blockName, lat, lon,
  grade, notes (≤200), style?, condition?, createdAt }
```

**`public_climbs/{climbId}`** — espejo público del diario (solo si
`isPublic`+`username`). `id = uid_sessionId`. Permite query "quién escaló
en X escuela".

**`sectores/{sectorId}/notas/{noteId}`** — notas comunitarias:
```
{ texto, autor, uid, fecha, votantesUp:[uid], votantesDown:[uid] }
```
Crear nota: solo vía CF `crearNota`. Votar: cualquier auth con regla
estricta (solo puede meter/sacar SU uid).

**`sector_submissions/{id}`** — propuesta de escuela nueva (solo vía CF
`submitSector`).

**`sectores_community/{id}`** — escuelas aprobadas (`approveSector`).

**`subsectores/{id}`** — bloques/piedras/zonas/parkings dibujados por admin
sobre el mapa de una escuela.

**`sector_overrides/{slug}`** — admin corrige coordenadas de sectores del
JSON estático.

**`pending_contributions/{id}`** — propuestas de usuarios:
```
{
  type: 'piedra' | 'zona' | 'sector' | 'parking' | 'sector_move' | 'topo_photo',
  status: 'pending' | 'approved' | 'rejected',
  parentSectorSlug, parentSectorName, parentLat, parentLon,
  name, lat, lon,
  // según type:
  bloques?: [{ name, grade }], fotoUrl?, fotoPath?, topoLineas?,
  descripcion?, proposedLat?, proposedLon?, reason?,
  // metadata
  createdBy, createdByName, createdByEmail, createdAt,
  reviewedAt?, reviewedBy?
}
```

**`admin_logs/{id}`** — log inmutable (solo create, no update/delete).

### Firebase Storage

- `profile-photos/{uid}` — foto de perfil (≤5 MB).
- `piedra-photos/{piedraId}.jpg` — foto de piedra aprobada.
- `piedra-photos-pending/{uid}_{timestamp}.jpg` — foto de propuesta pendiente.

### Cloud Functions (16 en total)

Schedulers:
- `sendDailyClimbingNotifications` — `onSchedule("0 * * * *")`. Cada hora,
  procesa usuarios con `notificationPrefs.hour == currentHour`, calcula
  forecast+score para los días seleccionados, manda push con top-3.
- `cleanupOldProposals` — `onSchedule("every day 03:00")`. Borra propuestas
  approved/rejected con >5 días.

Callable (auth):
- `submitSector` — usuario crea propuesta de escuela nueva.
- `crearNota` — usuario crea nota en un sector.

Callable (admin):
- `listAdminUsers` — cruza Firebase Auth con Firestore.
- `approveSector` — mueve submission a `sectores_community` (batch atómico
  + log).
- `listAdminNotes`, `deleteAdminNote` — gestión de notas vía
  `collectionGroup`.
- `sendAdminPush` — push manual (target user o broadcast).

Triggers Firestore:
- `onProposalReviewed` (onDocumentUpdated `pending_contributions/*`) —
  cuando admin marca approved/rejected, push + email al autor.
- `onNewFollower` (onDocumentCreated `followers/*`) — push al seguido.
- `onTopGradeIncreased` (onDocumentUpdated `users/*`) — push a sus
  seguidores cuando sube su `topGrade`.
- `onMessageCreated` (onDocumentCreated `messages/*`) — push al receptor
  del chat.
- `onFollowRequest` (onDocumentCreated `follow_requests/*`) — push de
  solicitud de seguimiento.
- `onFollowerCreated` / `onFollowerDeleted` — denormalizan
  `followersCount`/`followingCount`.

### Datos estáticos

- `data/sectores.json` / `sectores.json` (root) — catálogo base de escuelas
  servido desde `https://climbingteams.com/data/sectores.json`.

### Features REALES (todas ya existen, hay que migrar)

**Catálogo y mapa**
- ✅ Catálogo de escuelas con filtros (region, style, rockType, distance).
- ✅ Mapa interactivo de escuelas con marcadores por score (MapLibre).
- ✅ Detalle de escuela: tiempo + score + notas + análisis histórico
  + mapa topo de la escuela.
- ✅ **Bloques/piedras/zonas/parkings** dibujados sobre el mapa de cada
  escuela (admin con `block-editor.js`).
- ✅ **Editor topo**: foto de piedra + dibujar líneas con colores por
  grado + tipo de inicio (pie/sentado/lance/travesía).
- ✅ **Visor topo público**.

**Comunidad**
- ✅ Notas comunitarias por sector con votos up/down (idempotentes).
- ✅ Propuestas de escuelas nuevas (`submitSector` CF).
- ✅ **Sistema de propuestas community** (`pending_contributions`):
  piedras, zonas, parkings, sector_move, topo_photo.
- ✅ Admin review queue + aprobar/rechazar + push + email al autor.

**Usuarios**
- ✅ Login con Google (popup desktop/iOS-PWA, redirect Android).
- ✅ **Perfil público/privado** con `@username`, foto, bio, topGrade.
- ✅ **Búsqueda de usuarios** por username.
- ✅ **Follows** + solicitudes de seguimiento (para perfiles privados).
- ✅ **Diario personal de escalada** (`journal`): fecha, escuela, sector,
  bloque, grado, notas. Stats agregadas.
- ✅ **Diario público** (`public_climbs`): seguidores ven el diario
  completo del otro usuario.
- ✅ "Quién escaló aquí" — public_climbs por escuela.
- ✅ Favoritos cloud ↔ localStorage con dedup por proximidad lat/lon.

**Mensajería**
- ✅ **Chat directo 1-a-1** entre usuarios (`conversations/messages`).
- ✅ Filtro: solo puedes escribir a perfiles públicos o a alguien que te
  siga.

**Notificaciones**
- ✅ Push diarias programadas (top-3 sectores filtrados por prefs).
- ✅ Push de eventos: nuevo follower, follow request, mensaje, propuesta
  revisada, seguidor sube `topGrade`.
- ✅ **Bandeja de notificaciones** (`users/{uid}/notifications`).
- ✅ Configuración detallada (mode filters/sectors, hora, días, rocas,
  distancia).

**Otros**
- ✅ Comparador de hasta 5 favoritos (caché 15min).
- ✅ Deep-links de compartir (`?lat=&lon=&name=`).
- ✅ PWA con install prompt + service worker + offline (cachea escuelas,
  fotos, piedras, tiempo).
- ✅ Modo oscuro / claro con tokens CSS.
- ✅ Tema "Cumbre" (rediseño visual completo).
- ✅ Panel admin con tabs: stats, users, submissions, notes, push,
  community, sectors, pending review, activity.

### Duplicación a eliminar al migrar

- **Score**: `js/score.js` (front) **y** `functions/score.js` (idéntico,
  refactor reciente `feat: fuente única cliente+servidor`). Migrar a Java
  unifica TODO.
- **Haversine**: `js/utils/distance.js` y `functions/index.js`. Ya está
  en Java (`domain/util/GeoDistance`).
- **Rock drying profiles**: en cliente y en CF. Java lo unifica.

### Decisiones de arquitectura para el back

Mi recomendación senior tras ver el alcance real:

**A Postgres (datos del back Java):**
- escuelas (catálogo base + community), notas, votos, favoritos,
  perfiles, journal, public_climbs, follows, follow_requests,
  pending_contributions, subsectores (bloques/zonas/parkings),
  notifications_inbox.

**Queda en Firebase:**
- **Auth** — Firebase Auth + Spring Security valida JWT.
- **Chat** — Firestore (realtime esencial, replicar en Postgres sería
  reinventar la rueda).
- **Storage** — Firebase Storage para fotos (perfil, piedras).
- **Push** — FCM.
- **Triggers de chat / follow / message** — Cloud Functions (porque
  reaccionan a eventos Firestore directamente). El resto de CFs pasan a
  `@Scheduled` o webhooks en Spring.

### Preguntas abiertas que afectan al diseño de tablas

1. **Votos en notas**: hoy arrays de uids dentro del doc. En Postgres,
   tabla `note_votes(note_id, user_id, value)` permite contar y consultar
   "¿este usuario ya votó?" sin traer toda la nota.
2. **followersCount / followingCount**: denormalizados via CF en Firestore.
   En Postgres: ¿vistas materializadas? ¿columna con trigger? ¿`COUNT(*)`
   directo si los tamaños son pequeños?
3. **public_climbs vs journal**: hoy duplicado en Firestore por motivos
   de rules. En Postgres podemos servirlo con una query `JOIN
   users + journal WHERE isPublic = true`. ¿Lo unificamos?
4. **subsectores (bloques en mapa)** vs **pending_contributions de tipo
   piedra**: relacionados pero separados. ¿Modelo en Postgres con tabla
   única + estado, o dos tablas?
5. **Username**: Postgres puede garantizar unicidad con `UNIQUE INDEX`.
   Quitamos la colección `usernames` que era un workaround de Firestore.
6. **Premium**: ¿roadmap concreto o el flag queda dormido?
7. **Notificaciones inbox**: ¿las metemos en Postgres o se quedan en
   Firestore por simplicidad de los triggers existentes?

## Alcance completo del producto (visión)

El backend tiene que servir todas las funcionalidades de MeteoMontana cuando
acabe la migración. Lista de capacidades que sí cubre el back:

**Catálogo de escalada**
- Schools (escuelas) con metadatos: región, estilo, tipo de roca, ubicación,
  fuente.
- Filtros (región, estilo, roca, distancia).
- Notas públicas por school.
- Propuestas de nuevas schools por usuarios + flujo de aprobación admin.
- "Community schools" — schools aprobadas que conviven con el catálogo base.
- Fotos de bloques / sectores (storage TBD: Firebase Storage o S3-compatible).
- Mapa: el back devuelve coordenadas; el cliente renderiza con Leaflet u
  otra librería.

**Tiempo y scores**
- Forecast por school (combina Open-Meteo + scoring propio).
- Cálculo de score por hora y por día.
- Caché para no llamar a Open-Meteo en cada request.

**Usuarios**
- Perfil público (nombre, avatar, bio, fotos públicas).
- Perfil privado (preferencias, ubicación guardada, fcm token).
- Diario personal (journal) — entradas con texto, fecha, school asociada.
- Contributions: propuestas de schools / bloques nuevos.
- Follows: seguir a otros usuarios, lista de seguidores/siguiendo.
- Votos en notas (up/down, idempotente por usuario).

**Admin**
- Listar submissions pendientes.
- Approve/reject submissions con flujo atómico (mover propuesta a community
  schools, registrar log, notificar al autor).
- Log de actividad admin auditable.

**Notificaciones**
- FCM push: nueva nota en school favorita, contribution aprobada/rechazada,
  nuevo follower, etc.

**Lo que NO está en el back**
- Auth (Firebase Auth en el cliente).
- Chats entre usuarios (se quedan en Firestore por realtime).
- Renderizado UI, mapas Leaflet, service worker, localStorage.

## Mapa de migración por fases

### Fase 1 — Endpoints públicos de lectura (JSON estático) ✅ COMPLETA
- [x] `GET /api/escuelas`
- [x] `GET /api/escuelas/{id}`
- [x] Filtros: `?ccaa=`, `?estilo=`, `?roca=Caliza,Granito` (lista),
      `?lat=&lon=&radioKm=` (Haversine en `domain/util/GeoDistance`)

Patrones aprendidos: `param == null || compara(...)` en stream con
cortocircuito `||` para filtros opcionales. Listas con `List<String>` +
`anyMatch`. `Double` envoltorio en params opcionales numéricos.

### Fase 2 — Postgres + JPA (**aquí estamos**)
Levantar Postgres en Docker, añadir Spring Data JPA y Flyway, migrar
`escuelas` desde el JSON estático a una tabla real. El endpoint sigue
sirviendo igual hacia fuera; por dentro pasa a SQL. Aquí se introduce el
renombrado a inglés (`School` reemplaza a `Escuela` al aparecer).

- [ ] Docker Desktop instalado en Windows
- [ ] `docker-compose.yml` con Postgres 16
- [ ] Dependencias en `pom.xml`: `spring-boot-starter-data-jpa`,
      `postgresql`, `flyway-core`
- [ ] `application.properties`: datasource, JPA, Flyway
- [ ] Migración Flyway `V1__create_schools.sql`
- [ ] `SchoolJpaEntity` + `SchoolJpaRepository`
- [ ] Nueva implementación del port → adapta JPA al dominio
- [ ] Seed inicial: `CommandLineRunner` que importa `escuelas.json` a Postgres
- [ ] Deprecar/eliminar `JsonEscuelaRepository`

Aprendizaje: Docker básico, SQL básico, `@Entity`, `@Id`, `JpaRepository`,
Flyway, hexagonal con adaptadores reales.

### Fase 3 — Notas con Postgres
Tabla `notes` con FK a `schools`. Endpoint `GET /api/schools/{id}/notes`
público. Script standalone que migra notas desde Firestore a Postgres.

Aprendizaje: `@OneToMany`/`@ManyToOne`, FKs, índices, lazy loading,
migración de datos real.

### Fase 4 — Spring Security + Firebase Auth
Filtro que valida ID tokens de Firebase como JWT estándar usando JWKS de
Google. `@AuthenticationPrincipal` con un `MeteoUser` propio.

Endpoints autenticados:
- `POST /api/journal`
- `POST /api/contributions`
- `POST /api/follows/{uid}` / `DELETE /api/follows/{uid}`
- `PUT /api/profile`
- `POST /api/schools/{id}/notes`
- `POST /api/schools/{id}/notes/{noteId}/vote`

Aprendizaje: Spring Security, `OncePerRequestFilter`, JWT, JWKS, `@Valid`,
CORS.

### Fase 5 — Lógica pura: cálculo del score
Mover `js/score.js` y `js/score-core.js` a Java:
- `ClimbScoreCalculator`, `RockDryingProfile`, `HourlyScoreCalculator`,
  `DayAvgScoreCalculator`.

Aprendizaje: JUnit, `record`, streams avanzados, tests de lógica pura.

### Fase 6 — Tiempo: Open-Meteo + forecast endpoint
- `GET /api/schools/{id}/forecast` — DTO con school + tiempo + scores
  precocinados.

Aprendizaje: `RestClient`, DTO vs modelo de dominio, `@Cacheable`,
manejo de errores de servicios externos.

### Fase 7 — Fotos y storage
Subir fotos de bloques/schools.
- Decisión pendiente: Firebase Storage (más simple, ya usas Firebase) o
  S3/MinIO (más portable, más empleable). Recomendación senior: empezar con
  Firebase Storage por simplicidad, migrar si llegara a ser problema.
- Endpoint `POST /api/schools/{id}/photos` con multipart.

Aprendizaje: subida multipart, validación de imágenes, generación de URLs
firmadas.

### Fase 8 — Perfiles público / privado
- `GET /api/users/{uid}` — perfil público.
- `GET /api/me` — perfil privado del usuario autenticado.
- `PUT /api/me` — actualizar perfil.

Aprendizaje: DTOs distintos según audiencia, principio de mínimo privilegio
en datos expuestos.

### Fase 9 — Admin
- `GET /api/admin/submissions`
- `POST /api/admin/submissions/{id}/approve` — transacción multi-tabla.
- `POST /api/admin/submissions/{id}/reject`

Aprendizaje: roles con Spring Security, `@Transactional`, atomicidad.

### Fase 10 — Cloud Functions / FCM
Migrar CFs case-by-case. Las que mantienen contadores, notifican o ejecutan
en horario → `@Scheduled` en Spring + FCM Admin SDK.

Aprendizaje: `@Scheduled`, eventos asíncronos (`ApplicationEventPublisher`),
FCM desde Java.

### Fase 11 — Despliegue
Publicar el back para que el frontend en producción lo pueda usar.
Opciones: Railway, Render, Fly.io, GCP Cloud Run.
- CI/CD con GitHub Actions.
- Variables de entorno gestionadas.
- Postgres gestionado (Neon, Railway, Supabase como Postgres).

Aprendizaje: contenerización para prod, secrets management, observabilidad
básica (logs estructurados, healthchecks).

## Qué se queda SIEMPRE en el frontend

Firebase Auth SDK (login), renderizado HTML (`js/ui/`, `js/widgets/`,
`js/profile/`), state del navegador, mapas Leaflet, service worker,
localStorage, eventos DOM, **chats Firestore en realtime** (decisión
consciente).

## Bitácora reciente

(Las últimas 5-10 sesiones aproximadamente. Las más antiguas se podan.)

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
- **Fase 2.3 ✅**: primera migración Flyway `V1__create_schools.sql` aplicada.
  Tabla `schools` creada con 9 columnas (id, name, location, region, style,
  rock_type, lat, lon, source) + 3 índices (region, rock_type, style).
  `flyway_schema_history` contiene la fila de auditoría.

## Estado actual

**Fase 2, sub-paso 2.4 (siguiente)**: crear el modelo JPA `SchoolJpaEntity`,
su repositorio Spring Data JPA, y el adaptador que conecta con el port
`EscuelaRepository` del dominio.

**Decisión pendiente que el usuario debe entender bien antes de continuar:**

Hay dos formas de hacer 2.4:
- **Opción A (recomendada — hexagonal pura)**: `SchoolJpaEntity` es una
  CLASE NUEVA en `infrastructure/persistence/jpa/`, con las anotaciones
  JPA. `Escuela.java` (dominio) NO se toca, sigue siendo POJO puro. Un
  adaptador (`JpaSchoolRepositoryAdapter implements EscuelaRepository`)
  mapea entre `SchoolJpaEntity` y `Escuela`. Cuesta ~20 líneas de mapping
  pero mantiene el dominio libre de framework.
- **Opción B (pragmática)**: meter las anotaciones JPA directamente en
  `Escuela.java`. Menos código, pero acopla el dominio a JPA, exige
  constructor sin args y quitar `final` a los campos.

El usuario cerró la sesión SIN haber elegido — pidió volver a ver la
explicación más clara cuando regrese. **Próximo Claude: re-explica A vs B
con ejemplos concretos del código y deja que decida.** Una vez elegida,
sub-paso 2.4 arranca:

Si **A**:
1. Crear `infrastructure/persistence/jpa/SchoolJpaEntity.java` (con
   `@Entity`, `@Table(name="schools")`, `@Id`, etc.).
2. Crear `infrastructure/persistence/jpa/SpringDataSchoolRepository.java`
   (interfaz que `extends JpaRepository<SchoolJpaEntity, String>`).
3. Crear `infrastructure/persistence/jpa/JpaSchoolRepositoryAdapter.java`
   que implementa `EscuelaRepository` y delega en el Spring Data repo.
4. Anotar la nueva impl con `@Primary` o desactivar/borrar la antigua
   `JsonEscuelaRepository` (al principio podemos tenerlas ambas con
   `@Profile` para no romper Fase 1).

Si **B**:
1. Añadir anotaciones JPA a `Escuela.java` (`@Entity`, `@Table`, `@Id`).
2. Quitar `final` de los campos, añadir constructor sin args (puede ser
   `protected`).
3. Crear `infrastructure/persistence/jpa/EscuelaJpaRepository` que extiende
   `JpaRepository<Escuela, String>` y a la vez implementa el port
   `EscuelaRepository`.

Después de 2.4 viene **2.5 — Seed**: `CommandLineRunner` que lee
`escuelas.json` (sigue en `resources/`) y lo inserta en Postgres si está
vacío. Y luego **2.6 — Rename masivo a inglés** (Escuela → School, URLs,
etc.) como commit aparte.

**Bloqueo actual**: ninguno técnico. El usuario quiere recordatorio
claro del trade-off A vs B antes de seguir.

**Notas operativas para el siguiente Claude**:
- El usuario tiene **huecos en SQL básico** y **es la primera vez con
  Docker**. Ambos cubiertos hasta donde hemos llegado, pero JPA es
  terreno nuevo: explica `@Entity`, `@Id`, `@Column` desde cero.
- La contraseña Postgres está en `.env` (no commit). docker-compose y
  Spring la leen ambos via `${POSTGRES_PASSWORD}`.
- `application.yaml` tiene `spring.config.import: optional:file:../.env[.properties]`
  — depende del working dir = `api/`. Si IntelliJ corre desde otro
  sitio fallará. Solución apuntada: variable de entorno en Run Config.
- La app arranca con `cd api && ./mvnw spring-boot:run`. El contenedor
  Postgres se arranca con `docker compose up -d` desde la raíz.
- El frontend en `C:\Users\rouma\Desktop\MeteoMontana` evoluciona rápido;
  haz `git fetch && git status -sb` allí en cada sesión que vaya a tocar
  el front (NO necesario para sesiones puramente de back).
