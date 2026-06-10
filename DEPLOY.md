# Despliegue de MeteoMontanaAPI

Guía para hostear el backend Spring Boot + Postgres en **Railway** (recomendado),
Render o Fly.io. Una vez desplegado tendrás una URL HTTPS estable tipo
`https://api.climbingteams.com` que tu app Android/iOS consume en producción.

---

## 📋 Variables de entorno

| Variable | Requerida | Default | Descripción |
|---|---|---|---|
| `DATABASE_URL` | sí prod | `jdbc:postgresql://localhost:5432/meteomontana` | URL JDBC completa |
| `DATABASE_USERNAME` | dev | `meteomontana` | Usuario Postgres (Railway: `PGUSER` auto) |
| `POSTGRES_PASSWORD` | sí | — | Password Postgres (Railway: `PGPASSWORD` auto) |
| `PORT` | no | `8080` | Puerto HTTP |
| `FIREBASE_SA_JSON` | sí prod | — | **JSON entero** de `serviceAccountKey.json` |
| `FIREBASE_STORAGE_BUCKET` | no | `climbingteams.firebasestorage.app` | Bucket Storage |
| `RESEND_API_KEY` | opcional | — | API key de resend.com para emails |
| `RESEND_FROM` | opcional | `MeteoMontana <noreply@climbingteams.com>` | Remitente |
| `JPA_SHOW_SQL` | no | `false` | Dejar `false` en prod |
| `DB_POOL_SIZE` | no | `5` | Pool Hikari |

---

## 🔑 Credenciales Firebase

`FirebaseConfig.java` busca en este orden:

1. **Env var `FIREBASE_SA_JSON`** con el contenido JSON pegado tal cual → producción.
2. **Fichero `resources/serviceAccountKey.json`** → desarrollo local.

### Cómo pegar el JSON en Railway/Render

1. Abre `api/src/main/resources/serviceAccountKey.json`.
2. Copia su contenido entero (con `{` y `}`).
3. Panel Railway/Render → Variables → New variable:
   - Nombre: `FIREBASE_SA_JSON`
   - Valor: pega el JSON entero (acepta multilínea sin escapar).
4. Save → el servicio reinicia automáticamente.

⚠️ **NUNCA** subas el JSON al repo. Está en `.gitignore`.

---

## 🚀 Railway (recomendado)

### Setup inicial (5 minutos)

1. https://railway.app → crear cuenta con GitHub.
2. **New Project → Deploy from GitHub repo** → `MeteoMontanaAPI`.
3. Railway detecta `api/Dockerfile` y empieza a buildar.
4. Mientras builda, añade Postgres:
   - **+ New → Database → Add PostgreSQL**.
   - Railway crea la BD e inyecta `DATABASE_URL`, `PGUSER`, `PGPASSWORD`
     automáticamente al servicio.
5. Cuando el build acabe (~3-5 min), Railway expone la URL pública. Verifica:
   ```
   curl https://meteomontana-api.up.railway.app/actuator/health
   → {"status":"UP"}
   ```

### Variables de entorno extra

Variables → New variable:

```
FIREBASE_SA_JSON       = <pega el JSON>
RESEND_API_KEY         = re_xxxxxxxxxxxx  (opcional)
RESEND_FROM            = ClimbingTeams <noreply@climbingteams.com>
```

### Migrar datos dev → producción

En dev:

```powershell
docker exec -t meteomontanaapi_db_1 pg_dump -U meteomontana -d meteomontana > backup.sql
```

Conecta a Railway Postgres (cliente DBeaver/pgAdmin con las credenciales del
panel) y restaura:

```bash
psql "<connection_string_railway>" < backup.sql
```

O con la CLI:

```bash
npm i -g @railway/cli
railway login
railway link
railway run psql < backup.sql
```

### Dominio custom — `api.climbingteams.com`

1. Railway → Settings → Custom Domain → `api.climbingteams.com`.
2. Te da un CNAME a configurar en el panel de tu registrador:
   ```
   Tipo:   CNAME
   Nombre: api
   Valor:  <meteomontana-api.up.railway.app>
   TTL:    300
   ```
3. Esperar 5-30 min a la propagación. Railway emite HTTPS automático.
4. En Android `app/build.gradle.kts`:
   ```kotlin
   release {
       buildConfigField("String", "API_BASE_URL",
                        "\"https://api.climbingteams.com/api/\"")
   }
   ```

### Coste estimado

- Backend always-on (512MB RAM): ~$3-5/mes
- Postgres (1 GB storage): ~$5/mes
- **Total** ~$8-10/mes con tráfico bajo

---

## 🔄 Render (alternativa)

1. https://render.com → New → Web Service → conectar repo.
2. Root directory: `api/`. Render detecta `Dockerfile`.
3. + New → PostgreSQL → plan Starter ($7/mo).
4. Web Service → Environment → mismas env vars de arriba.

⚠️ Plan Free duerme tras 15 min sin tráfico. Para producción → Starter ($7/mo).
Total ~$14/mes (más caro que Railway).

---

## ✈️ Fly.io (más técnico, más barato)

```bash
brew install flyctl
fly launch                # detecta Dockerfile
fly secrets set FIREBASE_SA_JSON="$(cat api/src/main/resources/serviceAccountKey.json)"
fly secrets set RESEND_API_KEY="re_xxx"
fly postgres create
fly postgres attach
fly deploy
```

Free tier real, no caduca BD.

---

## ✅ Checklist pre-deploy

- [ ] `serviceAccountKey.json` NO commiteado: `git ls-files | grep serviceAccount` no devuelve nada.
- [ ] `application.yaml` lee env vars con fallback a `localhost` para dev.
- [ ] `FirebaseConfig.java` lee `FIREBASE_SA_JSON` primero.
- [ ] Dockerfile builda local: `cd api && docker build -t api .`
- [ ] `/actuator/health` público (verificar `SecurityConfig.java`).
- [ ] Migraciones Flyway listas para BD vacía.
- [ ] Backup Postgres dev hecho con `pg_dump`.

## 🆘 Troubleshooting

### "DATABASE_URL not found"
En Railway, el Postgres tiene que estar en el mismo proyecto.

### "Firebase credentials missing"
Falta `FIREBASE_SA_JSON`. Pégalo en Variables.

### Cold start lento
Spring Boot tarda ~30s en arrancar. Con plan Starter Railway es always-on
así que solo pasa una vez tras el deploy.

### Móvil no llega al backend
- Comprobar `network_security_config.xml` en Android no bloquea HTTPS.
- Comprobar `SecurityConfig.corsConfigurationSource()` permite tu origen.

---

## Tras el deploy

1. Verifica `https://api.climbingteams.com/actuator/health` desde el móvil/navegador.
2. Build release del Android apuntando al dominio nuevo y probar login + propuesta end-to-end.
3. Monitoriza logs en Railway → tab "Logs" durante los primeros días.
4. Cuando todo OK → Play Console Internal Testing.

---

## ✅ Estado actual (2026-06-10)

**Producción operativa**:
- Backend en `https://api.climbingteams.com` (Railway, plan trial $5 saldo).
- Postgres gestionada Railway con 191 escuelas migradas vía pg_dump --data-only.
- Contraseña Postgres **rotada** después del setup (la inicial se filtró en chat).
- Cloudflare proxy DESACTIVADO para el CNAME (DNS only) → HTTPS directo Railway.
- `serviceAccountKey.json` cargado via env var `FIREBASE_SA_JSON`.

**Variables Railway configuradas**:
```
FIREBASE_SA_JSON       = <JSON entero del service account>
DATABASE_URL           = jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
DATABASE_USERNAME      = ${{Postgres.PGUSER}}
POSTGRES_PASSWORD      = ${{Postgres.PGPASSWORD}}
```

**Variables opcionales NO activadas todavía**:
- `RESEND_API_KEY` + `RESEND_FROM` — sin esto los emails al aprobar/rechazar no se mandan.
- `FIREBASE_STORAGE_BUCKET` — usa el default `climbingteams.firebasestorage.app`.

**Pendiente backend**:
- Activar Resend (sacar API key en resend.com con `climbingteams.com` verificado).
- Activar Firebase Plan Blaze antes de Play Store (FCM ilimitado solo Blaze).
- Endpoint cacheado de stats mensuales (mover lógica de Open-Meteo archive a backend).

**Pendiente Android para publicar**:
- Apuntar `API_BASE_URL` release a `https://api.climbingteams.com/api/`.
- Keystore release + SHA-1 a Firebase Console.
- Build firmado `.aab` y subir a Play Console.

Detalle completo en `MeteoMontanaAndroid/DEPLOYMENT.md`.
