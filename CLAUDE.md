# MeteoMontanaAPI — contexto para Claude

Backend Spring Boot (Java) que sirve datos a la app Android y a la PWA
MeteoMontana.

---


## 📚 Documentos

| Documento | Qué es |
|---|---|
| **`HISTORIAL.md`** | bitácora por sesión: causas raíz, decisiones, trampas |
| `DEPLOY.md` | desplegar en Railway |
| `SCALING.md` | prueba de carga y capacidad medida |
| `INSTAGRAM_AUTOMATION.md` | automatización de publicaciones |
| `ARCHITECTURE.md` (repo Android) | reglas de arquitectura comunes |

## 🗺️ Mapa de repos — LEER PRIMERO

| Repo | Ruta local | GitHub |
|---|---|---|
| **Backend** (este repo) | `C:\Users\rouma\MeteoMontanaAPI` | `roumar1997/MeteoMontanaAPI` |
| **Android** | `C:\Users\rouma\MeteoMontanaAndroid` | `roumar1997/MeteoMontanaAndroid` |
| **PWA** JS (referencia) | `C:\Users\rouma\Desktop\MeteoMontana` | — |

**Los tres repos se trabajan juntos.** Cuando añades un endpoint aquí,
actualiza también el DTO y la interfaz Retrofit en `MeteoMontanaAndroid`.

---

## 🟢🟡 STAGING vs PRODUCCIÓN (desde 2026-06-22) — LEER

Hay **testers reales** en la prueba cerrada de Play usando producción EN VIVO.
**Regla nº1: pedir OK a Rodrigo antes de cualquier commit/merge a `main`** (un
cambio roto en prod les rompe la app). Flujo seguro con dos entornos Railway
(proyecto `zoological-wisdom`), cada uno con **su propia BD**:

| Entorno | Rama | URL | BD |
|---|---|---|---|
| **production** | `main` | `api.climbingteams.com` | datos reales (testers) |
| **staging** | `develop` | `meteomontanaapi-staging.up.railway.app` | copia del catálogo |

**Flujo**: cambias backend → push a **`develop`** → Railway despliega staging →
pruebas (los APK/`.ipa` *debug* de las apps apuntan a staging) → cuando va bien,
mergear a **`main`** (con OK) → Railway redespliega prod. Mantén los cambios
**retrocompatibles** (campos nullable/aditivos, migraciones solo aditivas) por las
apps ya instaladas. Firebase sigue compartido; lo aislado es backend + BD.

> Pendiente: rotar contraseñas de las dos BD en Railway (quedaron en un chat).

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

## Workflow de cada sesión

1. Lee este fichero: **Estado actual** te dice dónde estamos. Si el asunto
   tiene pasado, busca en `HISTORIAL.md` antes de sacar conclusiones.
2. Arranca sin repetir el plan entero.
3. Si el cambio toca backend Y app, **empieza por el backend**.
4. Al cerrar: una línea en `HISTORIAL.md` y **Estado actual** al día.

## Cómo trabaja el usuario

Desarrollador junior que quiere entender cada línea. Reglas:

1. **Idioma: español.** Código en **inglés** (clases, métodos, URLs, comentarios).
2. **SOLID + clean code + hexagonal, SIEMPRE** (regla de Rodrigo, 2026-07-20):
   todo lo que se cree, modifique o arregle respeta las capas de este repo y el
   `ARCHITECTURE.md` del repo Android, que es el documento normativo común. Lo
   que se toca se deja mejor (boy-scout); los atajos inevitables se avisan,
   nunca se cuelan en silencio.
3. **Paso a paso.** Un cambio cada vez, esperando confirmación.
4. **Verifica antes de proponer.** Lee el código antes de tocarlo, y comprueba
   contra el entorno desplegado antes de dar algo por bueno. **Al esperar un
   despliegue, la señal tiene que ser algo que SOLO pueda dar el commit nuevo.**
5. **Trade-offs explícitos** en las decisiones de diseño.
6. **Pide OK antes de commitear**, y con más razón aquí: hay usuarios reales en
   producción y Railway despliega `main` al instante.

**Tests**: `./mvnw test` verde antes de commitear.
`ApiApplicationTests.contextLoads` falla en el PC de Rodrigo por no haber
Postgres local — eso es esperado, cualquier otro rojo no.

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

## Estado actual

La API está **completa y en producción** desde 2026-07-10, sirviendo a las
apps de Play y App Store. La versión de Flyway vigente NO se apunta aquí
porque cambia cada semana: mírala en `api/src/main/resources/db/migration/`.

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

