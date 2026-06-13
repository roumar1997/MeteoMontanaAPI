# MeteoMontanaAPI — contexto para Claude

Backend Spring Boot (Java) que sirve datos a la app Android y a la PWA
MeteoMontana.

---

## 🗺️ Mapa de repos — LEER PRIMERO

| Repo | Ruta local | GitHub |
|---|---|---|
| **Backend** (este repo) | `C:\Users\rouma\MeteoMontanaAPI` | `roumar1997/MeteoMontanaAPI` |
| **Android** | `C:\Users\rouma\MeteoMontanaAndroid` | `roumar1997/MeteoMontanaAndroid` |
| **PWA** JS (referencia) | `C:\Users\rouma\Desktop\MeteoMontana` | — |

**Los tres repos se trabajan juntos.** Cuando añades un endpoint aquí,
actualiza también el DTO y la interfaz Retrofit en `MeteoMontanaAndroid`.

---

## ⚡ Arranque rápido

```powershell
# Desde la raíz de este repo:
docker compose up -d          # levanta Postgres 16 en puerto 5432
cd api
./mvnw spring-boot:run        # arranca el back en http://localhost:8080
                              # Flyway aplica migraciones V1..V12 automáticamente
```

**Variables de entorno** (en `.env`, excluido de git):
```
POSTGRES_PASSWORD=<tu_password>
# Opcionales (si no están, esas features no operan pero no rompen):
RESEND_API_KEY=re_xxxxxxxxxxxx          # email transaccional vía resend.com
RESEND_FROM=ClimbingTeams <noreply@climbingteams.com>
```

**Verificar:**
```
GET http://localhost:8080/actuator/health   →  {"status":"UP"}
GET http://localhost:8080/api/schools       →  191 escuelas
```

---

## 📁 Ficheros clave — dónde tocar qué

```
api/src/main/java/com/meteomontana/api/
  domain/
    model/       → entidades puras (School, PendingContribution, SchoolBlock...)
    port/        → interfaces de repositorio (SchoolRepository, etc.)
    exception/   → excepciones de dominio
    score/       → ClimbScoreCalculator, RockDryingProfile
    util/        → GeoDistance (Haversine)

  application/
    forecast/    → GetForecastUseCase, GetForecastByLocationUseCase,
                   ForecastResponse (expone weatherCode por hora)
    contribution/→ SubmitContributionUseCase, ReviewContributionUseCase,
                   ContributionRequest, ContributionResponse
                   ⚠️ ReviewContributionUseCase materializa la aprobación:
                   PARKING→BLOCK tipo PARKING, BOULDER→BLOCK, SECTOR→ZONE,
                   POSITION_CORRECTION→mueve bloque o escuela
    admin/       → AdminGuard: usar ensureAdmin(uid), NO check(user)
    submission/  → ApproveSubmissionUseCase (escuelas nuevas, diferente de contributions)
    [otros]      → favorites, follow, journal, notes, photos, profile, push...

  infrastructure/
    persistence/
      jpa/       → entidades JPA + SpringData repos
                   SpringDataSchoolBlockRepository — bloques del mapa
                   SpringDataContributionRepository — pending_contributions
                   SpringDataSchoolRepository — escuelas
    web/         → controllers REST
                   ContributionController → /api/schools/{id}/contributions
                   SchoolController, ForecastController, AdminController...
    weather/     → OpenMeteoClient (pide weatherCode entre otros campos)
                   OpenMeteoResponse (HourlyData con weatherCode)
    security/    → FirebaseTokenFilter, FirebaseUser record(uid, email, name)

  resources/
    db/migration/
      V1  — schools
      V2  — notes
      V3  — school_photos
      V4  — users
      V5  — school_submissions + admin_logs
      V6  — fcm_token en users
      V7  — journal
      V8  — favorites
      V9  — follows + notifications
      V10 — school_blocks (BLOCK/PARKING/ZONE) + block_lines
      V11 — pending_contributions (propuestas de mejora de escuelas existentes)
      V12 — target_block_id en pending_contributions
    serviceAccountKey.json  → Firebase credentials (NUNCA subir a git)
    application.yaml        → datasource, JPA, Flyway, caché
```

---

## 🔧 Patrones del proyecto (seguirlos siempre)

### Añadir un endpoint nuevo
1. Migración Flyway nueva (`V13__...sql`) si hay tabla nueva.
2. Modelo de dominio en `domain/model/`.
3. Puerto en `domain/port/` si hay repositorio.
4. Entidad JPA en `infrastructure/persistence/jpa/`.
5. SpringData repo en `infrastructure/persistence/`.
6. Use case en `application/`.
7. Controller en `infrastructure/web/`.
8. Actualizar `SecurityConfig` si el endpoint es público o necesita rol.
9. En Android: nuevo DTO en `data/api/dto/`, método en `SchoolApi`/`AdminApi`.

### AdminGuard
```java
// CORRECTO:
adminGuard.ensureAdmin(user.uid());
// INCORRECTO (no existe):
adminGuard.check(user);
```

### FirebaseUser
```java
// Los tres campos disponibles:
user.uid()    // String
user.email()  // String
user.name()   // String (displayName de Firebase)
```

### Compilar y verificar
```powershell
cd api
./mvnw compile          # solo compila
./mvnw spring-boot:run  # compila + arranca
```
Los errores de Flyway (`V_already_applied`) se resuelven con una migración
nueva, **nunca editando una ya aplicada**.

---

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

### Fase 2 — Postgres + JPA ✅ COMPLETA
- [x] Docker Desktop + `docker-compose.yml` con Postgres 16
- [x] Dependencias JPA + Postgres driver + Flyway en `pom.xml`
- [x] `application.yaml`: datasource, JPA (`ddl-auto: validate`), Flyway
- [x] Migración Flyway `V1__create_schools.sql` (tabla `schools` + índices)
- [x] `SchoolJpaEntity` + `SpringDataSchoolRepository`
- [x] `JpaSchoolRepositoryAdapter implements SchoolRepository` (hexagonal)
- [x] Seed inicial desde `escuelas.json` con `CommandLineRunner` (idempotente)
- [x] Rename completo a inglés: `School`, `SchoolRepository`, `SchoolController`,
      `GetSchoolsUseCase`, `GetSchoolByIdUseCase`, `SchoolNotFoundException`
- [x] URLs: `/api/schools`, `/api/schools/{id}`
- [x] Borrados: `JsonEscuelaRepository`, `FirebaseEscuelaRepository`, `Escuela.java`
      y todas las clases en español

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

## Estado actual

**TODAS LAS FASES DEL PLAN ORIGINAL ESTÁN COMPLETAS (1-11).**
**Migraciones Flyway hasta V24 aplicadas.**

**Endpoints implementados (resumen completo):**

*Públicos:*
- `GET /api/schools[?region&style&rockType&lat&lon&radioKm]`
- `GET /api/schools/{id}`
- `GET /api/schools/{id}/notes`
- `GET /api/schools/{id}/photos`
- `GET /api/schools/{id}/forecast` (cache; expone `weatherCode` WMO por hora;
  `current.drying` estima horas de secado por roca + viento/sol/temp/humedad,
  con suelo de 36h arenisca / 18h conglomerado)
- `GET /api/schools/{id}/blocks`
- `GET /api/users/{uid o username}`
- `GET /actuator/health`

*Auth (Bearer Firebase):*
- `GET /api/me` (JIT provisioning) | `PUT /api/me` | `PUT /api/me/fcm-token`
- `POST /api/schools/{id}/photos` | `DELETE /api/photos/{id}`
- `POST /api/submissions` | `GET /api/submissions/me`
- `POST /api/schools/{id}/contributions` — body: `{type, name?, lat, lon, notes?,
  description?, proposedLat?, proposedLon?, correctionReason?, targetBlockId?}`
  Tipos: `PARKING | BOULDER | SECTOR | POSITION_CORRECTION`
- `GET /api/contributions/me`
- `POST /api/schools/{id}/notes`
- `POST /api/journal` | `GET /api/journal/me` | `GET /api/journal/me/stats`

*Admin (`is_admin=true` en BD):*
- `GET /api/admin/submissions` | `POST .../approve|reject`
- `GET /api/admin/contributions` | `POST .../approve` | `POST .../reject`
- `GET /api/admin/logs` | `POST /api/admin/push`

**Materialización al aprobar contribución** (`ReviewContributionUseCase`):
- `PARKING` → crea `school_block` tipo `PARKING`
- `BOULDER` → crea `school_block` tipo `BLOCK`
- `SECTOR`  → crea `school_block` tipo `ZONE`
- `POSITION_CORRECTION` + `targetBlockId != null` → mueve ese bloque a `proposedLat/Lon`
- `POSITION_CORRECTION` + `targetBlockId = null`  → mueve la escuela entera

**TODOs técnicos acumulados** (mejoras, no bloqueantes):
- TTL en la caché del forecast (Caffeine con `expireAfterWrite=30m`).
- `POST /api/me/photo` para subir foto de perfil.
- `GET /api/users/search?q=...` — búsqueda de usuarios.
- Follows entre usuarios (tabla `follows`, endpoints, counts).
- Notas: votos (tabla `note_votes`).
- Migrar `FirebaseConfig` para leer `serviceAccountKey.json` desde filesystem
  cuando esté disponible (necesario en prod con volúmenes montados).

**Despliegue (Fase 11)**:
- Dockerfile y CI listos. Elegir proveedor (Railway/Render/Fly.io). Ver `DEPLOY.md`.

**Notas operativas para el siguiente Claude**:
- Contraseña Postgres en `.env` (no commit). Arranque: `docker compose up -d`
  desde raíz, luego `cd api && ./mvnw spring-boot:run`.
- 191 escuelas + notas + bloques/parkings en Postgres.
- Spring Security activo: GET `/api/schools/**`, `/api/users/**`,
  `/api/schools/*/blocks`, `/actuator/health` son públicos. Todo lo demás
  requiere `Authorization: Bearer <firebase-id-token>`.
- CORS configurado para localhost:5173, localhost:3000, 127.0.0.1:5500
  y climbingteams.com.
- `serviceAccountKey.json` en `api/src/main/resources/` Y en raíz. Excluidos de git.
- El usuario aprende paso a paso. Explica cada concepto desde cero.
