# Instagram automático de Cumbre (n8n)

Guía completa para publicar historias/posts en el Instagram **@cumbreclimbapp**
sin tocar nada a mano: n8n abre la URL de la historia → el renderizador la
convierte en PNG → se publica en Instagram.

```
   [n8n cron]  →  URL de la historia  →  [renderizador HTML→PNG]  →  Instagram Graph API
   (a una hora)   (api.climbingteams.com)   (mini-servicio Railway)     (publica la story)
```

Todo lo del BACKEND ya está hecho y en producción (los endpoints de datos +
los `/html` de las 5 historias). Falta desplegar el renderizador, la app de
Meta y montar n8n. Esta guía cubre esos 3 pasos.

---

## Estado (2026-07-17)

| Pieza | Estado |
|---|---|
| Datos de las 5 historias (`/api/social/*`) | ✅ en producción |
| HTML de las 5 historias (`/api/social/*/html`) | ✅ en producción |
| Renderizador `renderer/` (HTML→PNG) | ✅ código listo, **falta desplegar** |
| App de Meta + credenciales de IG | ⏳ pendiente (Rodrigo) |
| n8n + workflows | ⏳ pendiente |

Prerrequisitos YA hechos: IG @cumbreclimbapp es cuenta profesional (Empresa) y
está vinculado a la página de Facebook "Cumbre".

---

## Las 5 historias (URLs)

Todas devuelven un HTML de 1080×1920 con datos reales. El renderizador las
convierte en PNG.

| Historia | URL | Cuándo publicar |
|---|---|---|
| Condiciones de hoy | `/api/social/conditions/html?region={COMUNIDAD}` | diaria (mañana) |
| Novedades de la semana | `/api/social/novelties/html?days=7` | semanal |
| Ranking global | `/api/social/contributors/html` | mensual (día 1) |
| Ranking de la semana | `/api/social/contributors/html?days=7` | semanal (opcional) |
| Piedra nueva | `/api/social/newblock/html?postId={ID}` | al aprobar una piedra |

Regiones publicables (comunidades con ≥3 escuelas, sin rarezas):
`GET /api/social/regions` → lista de nombres para el bucle diario.

> **Nota de estrategia**: publicar la de condiciones de las ~15 comunidades cada
> mañana son muchas historias. Como las stories caducan en 24h no saturan el
> perfil, pero valora empezar solo con las comunidades con más escuelas
> (Cataluña, Madrid, Aragón, Andalucía…) y ampliar. Es cambiar el bucle de n8n.

---

## Paso 1 — Desplegar el renderizador en Railway

El renderizador (`renderer/` en este repo) es un mini-servicio Node + Chrome
que convierte una URL de historia en PNG. Aislado del backend de producción.

1. Railway → proyecto **`zoological-wisdom`** → **New → GitHub Repo** →
   `roumar1997/MeteoMontanaAPI`.
2. En **Settings** del servicio nuevo:
   - **Root Directory**: `renderer`
   - **Builder**: Dockerfile (lo detecta por el `Dockerfile`).
3. Deploy. Cuando termine: **Settings → Networking → Generate Domain**.
4. Prueba en el navegador: `https://<dominio-renderer>/health` → debe decir `ok`.
5. Prueba real (una historia a PNG):
   `https://<dominio-renderer>/render?url=<URL_CODIFICADA_DE_UNA_HISTORIA>`
   → debe descargar un PNG.

Guarda el dominio del renderizador: lo usará n8n. Lo llamaremos `{RENDERER}`.

---

## Paso 2 — App de Meta y credenciales de Instagram

Necesitas: el **ID de Instagram Business** de @cumbreclimbapp y un **token de
acceso de larga duración** con permisos de publicación.

### 2.1 Registro de desarrollador
- **developers.facebook.com** → Empezar → verifica (el código va por SMS al
  móvil de la cuenta). NO cambies el correo de contacto (deja el que ya tiene
  tu Facebook = roumar1997@gmail.com), o Facebook bloquea por "dispositivo
  poco usado".

### 2.2 Crear la app
- Mis apps → Crear app → tipo **Empresa/Business** → nombre `Cumbre Publisher`.
- Añade el producto **Instagram** (Graph API / Instagram API con Facebook Login).

### 2.3 Permisos necesarios
- `instagram_basic`
- `instagram_content_publish`  ← el clave (publicar)
- `pages_show_list`, `pages_read_engagement`, `business_management`

### 2.4 Obtener el Instagram Business Account ID
Con el **Graph API Explorer** (developers.facebook.com/tools/explorer),
seleccionando la app y un token de usuario con los permisos de arriba:
```
GET /me/accounts                         → coge el {page-id} de la página Cumbre
GET /{page-id}?fields=instagram_business_account   → devuelve el {ig-user-id}
```
Guarda `{ig-user-id}` (lo llamaremos `{IG_USER_ID}`).

### 2.5 Token de larga duración (de PÁGINA)
Los tokens cortos caducan en horas. Hay que canjear a uno largo (~60 días) y,
mejor, obtener un **token de página** (que no caduca mientras la app viva):
```
# 1) short user token (del Explorer) → long-lived user token (60 días):
GET /oauth/access_token?grant_type=fb_exchange_token
    &client_id={APP_ID}&client_secret={APP_SECRET}&fb_exchange_token={SHORT_TOKEN}
# 2) con el long-lived user token, pide el token de PÁGINA:
GET /{page-id}?fields=access_token&access_token={LONG_USER_TOKEN}
```
Guarda ese token de página como `{IG_TOKEN}`.

> Ojo: para publicar de verdad (no solo en modo prueba), la app tendrá que
> pasar **App Review** de Meta pidiendo `instagram_content_publish`. Con la
> cuenta en modo desarrollo se puede publicar en cuentas de prueba / la propia;
> revisar el estado actual de los requisitos de Meta al llegar aquí.

---

## Paso 3 — Publicar en Instagram (el flujo de la API)

Para CADA historia, dos llamadas:

```
# 1) Crear el contenedor (Instagram descarga la imagen de image_url):
POST https://graph.facebook.com/v21.0/{IG_USER_ID}/media
     ?image_url={URL_DEL_PNG}
     &media_type=STORIES
     &access_token={IG_TOKEN}
  → devuelve { "id": "{CREATION_ID}" }

# 2) Publicar el contenedor:
POST https://graph.facebook.com/v21.0/{IG_USER_ID}/media_publish
     ?creation_id={CREATION_ID}
     &access_token={IG_TOKEN}
  → publica la story
```

Donde `{URL_DEL_PNG}` = la URL del renderizador, p.ej.:
```
{RENDERER}/render?url={URL_HISTORIA_CODIFICADA}
```
Instagram descarga ESA url y obtiene el PNG. Para un POST de feed (no story)
en vez de historia, quita `media_type=STORIES` y añade `caption={texto}`.

---

## Paso 4 — Montar n8n en Railway

1. Railway → `zoological-wisdom` → **New → Template** → busca **n8n** (hay
   plantilla oficial) → deploy. (O imagen `n8nio/n8n`.)
2. Añade una base de datos si la plantilla la pide (n8n guarda sus workflows).
3. Variables de entorno útiles: `N8N_HOST`, `WEBHOOK_URL` (su dominio Railway),
   `GENERIC_TIMEZONE=Europe/Madrid`, y credenciales de acceso básico
   (`N8N_BASIC_AUTH_ACTIVE=true` + usuario/clave) para que no sea público.
4. Abre el dominio de n8n → crea tu cuenta de admin.
5. Importa el workflow: **Workflows → Import from File** →
   `renderer/n8n-conditions.json` (en este repo). Ajusta los valores:
   - `{API}` = `https://api.climbingteams.com`
   - `{RENDERER}` = dominio del Paso 1
   - `{IG_USER_ID}` y `{IG_TOKEN}` = del Paso 2 (mejor como *credenciales* de n8n)
6. Ejecuta el workflow a mano una vez (**Execute Workflow**) para probar que
   publica. Luego actívalo (el cron se encarga).

Replica el patrón para novedades / ranking / piedra nueva cambiando la URL de
la historia y el cron. La de **piedra nueva** es por evento: lo más simple es
un nodo cron cada X horas que pida `/api/feed?scope=explore` y publique los
`NEW_BLOCK` que no haya publicado aún (guardar los ids ya publicados en n8n),
o —mejor a futuro— un webhook que el backend llame al aprobar la contribución.

---

## Costes

- Renderizador: contenedor pequeño en Railway (~3-5 €/mes).
- n8n: contenedor pequeño (~5 €/mes) + su base de datos.
- Meta API: gratis.
Total ~10 €/mes. Todo self-hosted, sin límites de terceros.
