# Despliegue de MeteoMontanaAPI

## Resumen

- **Imagen**: multi-stage Dockerfile en `api/Dockerfile` con JDK 21 build + JRE
  21 runtime.
- **Healthcheck**: `GET /actuator/health` (público).
- **Base de datos**: Postgres 16. URL inyectada vía `DATABASE_URL`.
- **Secrets requeridos**: `POSTGRES_PASSWORD`, `serviceAccountKey.json` (Firebase).

## Variables de entorno

| Variable | Requerida | Default | Descripción |
|---|---|---|---|
| `DATABASE_URL` | sí en prod | `jdbc:postgresql://localhost:5432/meteomontana` | URL JDBC completa |
| `DATABASE_USERNAME` | sí en prod | `meteomontana` | Usuario de Postgres |
| `POSTGRES_PASSWORD` | sí | — | Contraseña de Postgres |
| `PORT` | no | `8080` | Puerto HTTP |
| `JPA_SHOW_SQL` | no | `true` (dev) | Poner `false` en prod |

## Service account de Firebase

El archivo `serviceAccountKey.json` **no se incluye en la imagen** por
seguridad. Hay que inyectarlo en runtime de una de estas formas:

1. **Volumen** (Docker, Fly.io): montar el archivo en
   `/app/serviceAccountKey.json` y usar la env var
   `GOOGLE_APPLICATION_CREDENTIALS=/app/serviceAccountKey.json`.
2. **Variable Base64** (Railway/Render): codificar el JSON en base64 y
   meterlo como `FIREBASE_SERVICE_ACCOUNT_B64`. Habrá que añadir al arranque
   un paso que lo decodifique al filesystem.

Por ahora `FirebaseConfig` carga el archivo desde el classpath, así que en
prod lo más fácil es **mapear como volumen** y modificar `FirebaseConfig`
para leer también desde una ruta del filesystem cuando exista.

## Opciones de hosting recomendadas

### Railway (más sencillo)

1. Crear proyecto en railway.app, conectar este repo.
2. Añadir servicio Postgres → Railway lo aprovisiona y rellena
   `DATABASE_URL` automáticamente.
3. Variables de entorno: `POSTGRES_PASSWORD`, `JPA_SHOW_SQL=false`.
4. Subir `serviceAccountKey.json` como volumen.
5. Railway detecta el Dockerfile y construye.
6. Healthcheck: `/actuator/health`.

### Render

Similar a Railway. `render.yaml` opcional para infra como código.

### Fly.io

Más control y red privada para Postgres. `fly launch` desde `api/`.

## CI/CD

`.github/workflows/ci.yml` ya configurado:
- Corre tests con Postgres real (service container).
- Compila el JAR.
- En PR: bloquea merge si falla.
- En push a `main`: corre la build.

**Pendiente**: añadir job de build + push de imagen Docker al registry
del proveedor cuando esté decidido.

## Local con Docker (test del Dockerfile)

```bash
cd api
docker build -t meteomontana-api .
docker run --rm -p 8080:8080 \
  -e POSTGRES_PASSWORD=$POSTGRES_PASSWORD \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/meteomontana \
  -v /ruta/a/serviceAccountKey.json:/app/serviceAccountKey.json:ro \
  meteomontana-api
```
