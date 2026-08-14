# SCHOOLS_SEED_PROGRESS.md — catálogo de piedras/parkings/sectores

> Lista viva. Se actualiza cada vez que se completa o toca una escuela.
> Fuente de datos: TheCrag (thecrag.com) y thetopo.com — metodología probada
> el 2026-08-13 con Santa Gadea (primera escuela hecha, ver HISTORIAL.md).
>
> **Regla de oro**: nunca inventar una coordenada. Si la fuente dice "no sabemos
> dónde está", se deja fuera y se apunta como pendiente, no se aproxima.
>
> **TheCrag bloquea por región** tras 10-30 peticiones seguidas (redirige a login).
> El bloqueo es por región de TheCrag, no global — otra región puede seguir
> funcionando. Si una está bloqueada, saltar a otra y volver luego.
>
> **LECCIÓN encoding (2026-08-14)**: un nombre con tildes/ñ ("Peña del Águila")
> enviado con `curl -d '...'` desde bash dio **500 Error interno** — no era un
> bug real del backend, era el shell mandando los bytes mal. Fix: escribir el
> JSON a un fichero con Python (`json.dump(..., ensure_ascii=False)`, UTF-8) y
> mandarlo con `curl --data-binary @payload.json`. Si un POST con acentos da
> 500, probar esto ANTES de sospechar del backend.
>
> **Pendiente futuro** (no ahora): botón B/V en el mapa de una escuela mixta para
> separar vista de bloque y de vía. No se toca el campo `style` de la escuela por
> ahora — cada piedra ya lleva su `discipline` (BOULDER/ROUTE) real por separado.
>
> **Fuentes alternativas probadas y descartadas (2026-08-13)** — no repetir esta
> búsqueda, ya está hecha:
> - **API oficial de TheCrag**: cerrada para uso no comercial sin acuerdo legal.
>   Las claves personales solo dan el propio libro de ascensos, no el catálogo.
> - **OpenBeta** (api.openbeta.io, GraphQL, gratis y sin bloqueo): cobertura de
>   España muy fina — solo tiene los sitios de referencia mundial (Siurana,
>   Margalef, El Chorro). Comprobado contra las 200 escuelas de la BD: **0
>   coincidencias**. Además tiene su propio límite (403 tras muchas peticiones
>   seguidas). No usar como fuente principal.
> - **Iniciar sesión en TheCrag con cuenta real**: descartado a propósito. El
>   throttle menciona `utm_id=anonsession`, sugiriendo que es un límite de
>   sesión anónima que subiría con cuenta — pero el patrón de navegación
>   automatizada sería fácil de detectar y podría acabar en bloqueo de esa
>   cuenta/IP. No arriesgar la cuenta personal de Rodrigo por esto.
> - **Blogs locales** (bulderahedo.blogspot.com, ahedoboulder.blogspot.com):
>   tienen fotocroquis y nombres de vías reales (útiles más adelante para
>   descripciones), pero SIN coordenadas GPS estructuradas.
> - **escaladaburgos.wordpress.com**: promociona una guía de papel de pago
>   (20€), no una base de datos digital. No cubre Sedano ni Peña Amaya. Sí
>   confirma que estas 12 escuelas existen y están en TheCrag (región
>   `toledo-salamanca-area`, bloqueada ahora mismo): Villaverde de Peñahorada,
>   Tobes, San Martín de Ubierna, La Selva, Huérmeces, La Piedra, Basconcillos
>   del Tozo, Poza de la Sal, Trespaderne, Villarcayo (Escaño y Escanduso),
>   Pancorbo, Garganchón. San Martín de Ubierna, Huérmeces y Pancorbo tienen
>   regulación de acceso específica (nidificación) — usar en la descripción
>   cuando se añadan.
> - **Conclusión**: TheCrag + thetopo.com siguen siendo las únicas fuentes
>   viables. Sin atajo. Ir despacio, región a región, sin cuenta.

## Resumen

- **Completas** (4+ elementos, no se tocan): 11
- **Con poco** (1-3 elementos, revisar/ampliar): 14
- **Vacías** (0 elementos): 175
- **Total**: 200

## HECHAS en esta ronda (2026-08-13)

| Escuela | Insertado | Pendiente |
|---|---|---|
| santa-gadea | 1 parking + 7 sectores | 6 sectores sin coordenada en ninguna fuente |
| resconorio | 1 parking + 4 sectores | completa |
| rozas | 2 sectores (ya tenía parking) | completa |
| recuevas | 1 parking + sectores (+ Columpio, Balcón, Sombra, Cuartos añadidos hoy) | Pendientes de la lista previa: Placas, Roble, Tochos, Halcones, Blanco, Verde, Negro, Pino (recuento exacto /22 por confirmar, la nota anterior tenía "7 más" pero listaba 11 nombres — revisar con calma la próxima vez). Siguiente: Placas |
| las-tuerces | **COMPLETA** — 1 parking + 1/13 sectores | 12 sectores confirmados SIN ubicación en TheCrag (Verdugo, Cave Canem, Techo del Camino, Callejón del Traidor, El Virgen, Balcon, Escajos, Callejón del Viento, Vivac y Muro de las Lamentaciones, Calle de la Amargura, Zona Del Juc — no volver a consultar). Coordenada de la escuela discrepa ~4km, ver Dudas |
| la-cabrera | **COMPLETA** — 28/29 de la lista original de vía + 5/14 sub-sectores de bloque (El merendero norte, El merendero sur, El merendero este, Sector M-124, Peña del Buey) = 33 sectores en total (+ Las agujas del convento, Risco del Fraile, Risco del Pajarito, Cancho Gordo, Atisbadero y Aguja de Venus, Perfil de Baco y El Castillo, La Fortaleza, Cancho de la Bola, Aguja de los Alquimistas, Cancho del Rayo y Cuerno de la Luna, Aguja Solano, Peña del Águila, Aguja del Pornoso, Agujas de los Campanarios, Pared de los Tubos, Aguja del Callejón, Aguja de los tres amigos y Trono, Aguja sin nombre, Risco del Murciélago, La Pirámide, El semicírculo, Cancho cuadrado, Cancho de los Tejos, Torre de los Casares, Cancho de las Yegüas, Bloque Californiano, Pico de la Miel, Cancho Soyermo) | Los otros 9 sub-sectores del árbol "La Cabrera - Bouldering" (Sectores del Murillón, Sector PR-M-13, Falda sur del Pico de la Miel, El Puticlub, Sector A-1, La Ventana/Camping, Las Pilatas, Fuente Caldera, San Pedro) tienen **0 vías/bloques en TheCrag** — comprobados uno a uno, son placeholders vacíos sin contenido, no se insertan (no volver a consultar). No se añadió 2º parking de la escuela: TheCrag da un punto a ~1,7km del "Parking Merendero Norte" ya existente, no está claro si es el mismo acceso u otro distinto — no decidido, no insertado |
| ahedo | 1 parking + 1/12 sectores | 10 más: Ley de Ohm, Falanche Man, Rinconzuco, Vaguada, Ciervo, Caverna, Invernal, Jungle, Vascongadas, Cueva |

## EN CURSO / bloqueadas por TheCrag ahora mismo

Región TheCrag `toledo-salamanca-area` (cubre Ahedo, Recuevas — Las Tuerces ya
COMPLETA) bloqueada desde el 2026-08-13, se destensa y vuelve a bloquear de forma
errática (a veces aguanta 8-10 peticiones, a veces 2).

**2026-08-14 (retomada, sin bloqueo)**: TheCrag respondió bien esta tanda.
Insertados 3 sectores más en Recuevas (verificados en la API pública): Balcón
(id 5962139553, 42.747802,-4.224513), Sombra (id 6541178814,
42.747754,-4.225896), Cuartos (id 6541178880, 42.748113,-4.226164).
Siguiente: "Placas".

**3 tandas seguidas bloqueadas de primeras aquí (2026-08-13).** Estrategia
confirmada y en marcha: si esta región bloquea al instante, saltar a otra (la
región `madrid-area`/`la-cabrera` de TheCrag sí responde bien, en curso ahora en
`la-cabrera`).

**2026-08-14 (avance)**: `la-cabrera`/10389887367 (Risco del Fraile, aka Cancho
de la Ladera) ya NO bloqueaba — insertado como ZONE en 40.872674,-3.638295
(verificado en la API pública). Siguiente sector candidato: "Risco del
Pajarito" (id aproximado 10389890034, sin confirmar — verificar entrando a la
página de la-cabrera y comprobando el nombre real antes de tocar nada).

**2026-08-14 (tanda grande, sin bloqueo)**: TheCrag siguió respondiendo bien
en `la-cabrera` toda la tanda. Insertados 4 sectores más (todos verificados en
la API pública): Risco del Pajarito (aka Cancho largo, id 5378034747,
40.873221,-3.639236), Cancho Gordo (id 11500687617, 40.875354,-3.639846),
Atisbadero y Aguja de Venus (id 11500715118, 40.876815,-3.635308), Perfil de
Baco y El Castillo (id 11501328873, 40.876625,-3.634285). La-cabrera pasa de
2/29 a 6/29 sectores. Siguiente sector en la tabla de la-cabrera: "La
Fortaleza" (área #7).

**2026-08-14 (otra tanda grande, sin bloqueo)**: TheCrag siguió respondiendo
bien. Insertados 3 sectores más (verificados en la API pública): La Fortaleza
(id 11500742763, 40.876718,-3.633648), Cancho de la Bola (id 11500742853,
40.876802,-3.632552), Aguja de los Alquimistas (id 11500742943,
40.876792,-3.631790). La-cabrera pasa de 6/29 a 9/29 sectores. Siguiente
sector en la tabla: "Cancho del Rayo y Cuerno de la Luna / Cancho de la
Ventana Alta y Baja" (área #10, id aproximado 11500743030, sin confirmar).

**2026-08-14 (tanda siguiente, sin bloqueo)**: TheCrag siguió respondiendo
bien. Insertados 2 sectores más (verificados en la API pública): Cancho del
Rayo y Cuerno de la Luna (aka Cancho de la Ventana Alta y Baja, id
11500743030, 40.876706,-3.631232), Aguja Solano (aka Punta Reventona, id
11500819596, 40.877600,-3.630370). La-cabrera pasa de 9/29 a 11/29 sectores.
Siguiente sector en la tabla: "Peña del Águila / Cancho del Águila" (área
#12).

**2026-08-14 (otra tanda, sin bloqueo — encoding cazado)**: TheCrag siguió
respondiendo bien. Insertados 2 sectores más (verificados en la API pública):
Peña del Águila (aka Cancho del Águila, id 6599945841, 40.877547,-3.626116),
Aguja del Pornoso (id 11500743114, 40.879203,-3.624299). La-cabrera pasa de
11/29 a 13/29 sectores. **De paso se cazó y corrigió un fallo propio**: el
primer intento de insertar "Peña del Águila" dio 500 por mandar el nombre con
tildes vía `curl -d` desde bash (encoding roto, no era bug del backend) — creó
sin querer un bloque mal escrito "Pena del Aguila" que se borró
(`DELETE /api/blocks/{id}`, 204) antes de reinsertar bien con
`--data-binary @payload.json` (ver nota de encoding al principio del
fichero). Siguiente sector en la tabla: "Agujas de los Campanarios" (área
#14).

**2026-08-14 (otra tanda más, sin bloqueo)**: TheCrag siguió respondiendo
bien. Insertados 2 sectores más (verificados en la API pública): Agujas de
los Campanarios (id 11501185845, 40.879140,-3.621060), Pared de los Tubos (id
11501185905, 40.879664,-3.620769). La-cabrera pasa de 13/29 a 15/29 sectores.
Siguiente sector en la tabla: "Aguja del Callejón" (área #16).

**2026-08-14 (siguiente tanda, sin bloqueo)**: TheCrag siguió respondiendo
bien. Insertados 2 sectores más (verificados en la API pública): Aguja del
Callejón (id 11501472198, 40.876655,-3.620196), Aguja de los tres amigos y
Trono (id 5377807404, 40.878949,-3.619809). La-cabrera pasa de 15/29 a 17/29
sectores. Siguiente sector en la tabla: "Aguja sin nombre / Aguja de los
tejos" (área #18).

**2026-08-14 (siguiente tanda más, sin bloqueo)**: TheCrag siguió respondiendo
bien. Insertados 2 sectores más (verificados en la API pública): Aguja sin
nombre (aka Aguja de los tejos, id 5377880517, 40.878417,-3.618621), Risco
del Murciélago (id 5378034687, 40.879138,-3.617832). La-cabrera pasa de 17/29
a 19/29 sectores. Siguiente sector en la tabla: "La Pirámide" (área #20).

**2026-08-14 (más tanda, sin bloqueo)**: TheCrag siguió respondiendo bien.
Insertados 2 sectores más (verificados en la API pública): La Pirámide (id
6599948154, 40.878615,-3.617541), El semicírculo (id 6599820138,
40.878069,-3.620169). La-cabrera pasa de 19/29 a 21/29 sectores. Siguiente
sector en la tabla: "Cancho cuadrado" (área #22).

**2026-08-14 (más tanda todavía, sin bloqueo)**: TheCrag siguió respondiendo
bien. Insertados 2 sectores más (verificados en la API pública): Cancho
cuadrado (id 6599931159, 40.879134,-3.618760), Cancho de los Tejos (id
11501183802, 40.878890,-3.616418). La-cabrera pasa de 21/29 a 23/29 sectores.
Siguiente sector en la tabla: "Torre de los Casares" (área #24).

**2026-08-14 (última tanda hasta ahora, sin bloqueo)**: TheCrag siguió
respondiendo bien. Insertados 2 sectores más (verificados en la API pública):
Torre de los Casares (id 11501034123, 40.879228,-3.614824), Cancho de las
Yegüas (id 9123331083, 40.879178,-3.609907). La-cabrera pasa de 23/29 a
25/29 sectores. Siguiente sector en la tabla: "Bloque Californiano" (área
#26).

**2026-08-14 (última tanda, sin bloqueo — pausa temporal)**: TheCrag siguió
respondiendo bien. Insertados 2 sectores más (verificados en la API
pública): Bloque Californiano (id 4287934290, 40.877168,-3.607981), Pico de
la Miel (id 3514726056, 40.878836,-3.608330). La-cabrera pasa de 25/29 a
27/29 sectores. Quedan solo 2: "Cancho Soyermo" (área #28) y "La Cabrera -
Bouldering" (área #29, probablemente una zona de bulder aparte, revisar
nombre/tipo con calma). Se pausa esta línea de trabajo para priorizar
desarrollo de producto (features iOS/Android) — retomar cuando toque.

**2026-08-14 (retomada, sin bloqueo)**: TheCrag siguió respondiendo bien.
Insertado Cancho Soyermo (id 11500747170, 40.879778,-3.607104) — verificado
en la API pública. La-cabrera pasa de 27/29 a **28/29 de la lista original**.
El item #29 "La Cabrera - Bouldering" (id 6599966469) resultó ser un área
CONTENEDORA con 14 sub-sectores propios sin insertar todavía: El merendero
norte, El merendero sur, El merendero este, Sector M-124, Peña del Buey,
Sectores del Murillón, Sector PR-M-13, Falda sur del Pico de la Miel, El
Puticlub, Sector A-1, La Ventana / Camping, Las Pilatas, Fuente Caldera, San
Pedro. Ninguno insertado esta tanda (había que abrir cada uno y sacar su
propio enlace de Google Maps, y ya no daba tiempo en la tanda) — siguiente
paso: entrar en https://www.thecrag.com/en/climbing/spain/la-cabrera/area/6599966469
y comprobar/insertar uno a uno empezando por "El merendero norte".

**2026-08-13 (tanda extra)**: bloqueo ahora es GLOBAL, no solo de una región —
probado `toledo-salamanca-area`/Balcón, la home de thecrag.com y
`la-cabrera`/10389887367, los tres a "Iniciar sesión para participar" al
instante. Sin avance esta tanda, nada insertado.

**2026-08-13 (otra tanda más)**: sigue bloqueado globalmente, reprobado
`la-cabrera`/10389887367 → login wall al instante. Sin avance, nada insertado.

**2026-08-13 (tanda siguiente)**: mismo resultado, `la-cabrera`/10389887367
sigue en login wall al instante. Sin avance, nada insertado.

**2026-08-13 (otra tanda)**: mismo resultado de nuevo, `la-cabrera`/10389887367
en login wall al instante. Sin avance, nada insertado.

**2026-08-14**: sigue igual, `la-cabrera`/10389887367 en login wall al
instante. Sin avance, nada insertado.

**2026-08-13 (nueva tanda)**: sigue igual, `la-cabrera`/10389887367 en login
wall al instante. Sin avance, nada insertado.

**2026-08-14**: sigue igual, `la-cabrera`/10389887367 en login wall al
instante. Sin avance, nada insertado.


**2026-08-14 (otra tanda)**: mismo resultado de nuevo, `la-cabrera`/10389887367 en login wall al instante. Sin avance, nada insertado.
**2026-08-14 (tanda siguiente)**: mismo resultado, `la-cabrera`/10389887367 en login wall al instante. Sin avance, nada insertado.

**2026-08-14 (otra tanda más)**: mismo resultado, `la-cabrera`/10389887367 en login wall al instante. Sin avance, nada insertado.

**2026-08-14 (nueva tanda, bloqueo distinto)**: esta vez `thecrag.com` está
bloqueado por **política del propio Browser pane del entorno** ("blocked by
policy"), no por el muro de login habitual de TheCrag — ni siquiera carga la
home. No es el mismo tipo de bloqueo de otras tandas (ese se destensaba solo);
este parece un bloqueo de dominio a nivel de entorno. Sin avance, nada
insertado. Siguiente objetivo en cuanto se pueda navegar: Recuevas → sector
"Placas" (17 sectores ya insertados y verificados en la API pública; quedan
Placas, Roble, Tochos, Halcones, Blanco, Verde, Negro, Pino de la lista
previa, recuento exacto por confirmar contra la página real).

**2026-08-14 (nueva tanda, sigue el mismo bloqueo)**: reprobado
`thecrag.com` — sigue "blocked by policy" en el Browser pane, no ha cambiado
desde la tanda anterior. Sin avance, nada insertado. Mismo siguiente
objetivo: Recuevas → "Placas".

**2026-08-14 (otra tanda, mismo bloqueo persistente)**: reprobado de nuevo,
`thecrag.com` sigue "blocked by policy" en el Browser pane de este entorno.
Ya van varias tandas seguidas así — parece un bloqueo estable a nivel de
entorno, no algo que se destense solo como el muro de login de TheCrag. Sin
avance, nada insertado. Mismo siguiente objetivo: Recuevas → "Placas".

**2026-08-14 (nueva tanda, bloqueo sigue igual)**: reprobado otra vez,
`thecrag.com` sigue "blocked by policy". Sin avance, nada insertado. Mismo
siguiente objetivo: Recuevas → "Placas".

**2026-08-14 (otra tanda, sin cambios)**: reprobado una vez más, mismo
"blocked by policy". Sin avance, nada insertado. Mismo siguiente objetivo:
Recuevas → "Placas".

**2026-08-14 (bloqueo distinto — vuelve el muro de login)**: el bloqueo de
política del Browser pane se ha levantado (la home de thecrag.com cargó
bien, sin login). Pero al navegar a la búsqueda de "Recuevas"
(`/en/climbing/spain?q=Recuevas`) saltó directo el muro habitual
"Iniciar sesión para participar" — se paró la tanda al instante, sin
reintentar, según la regla de siempre. Sin avance, nada insertado. Mismo
siguiente objetivo: Recuevas → "Placas".

**2026-08-14 (prueba "ir más despacio", descartada como causa)**: Rodrigo
sugirió que quizás el ritmo de navegación (URLs directas, sin pausas) se
detecta como bot. Probado con pasos MUY lentos y humanos: cargar home →
esperar 4s → aceptar cookies → esperar 3s → CLIC en la caja de búsqueda de
verdad (no URL adivinada) → esperar → escribir "Recuevas" letra a letra vía
`type` → esperar 2s → CLIC en el botón "Vamos" → esperar 3s. Mismo
resultado: muro de login instantáneo en cuanto se envía la búsqueda. **La
home SIEMPRE carga bien sin login; es la acción de buscar/consultar
contenido concreto la que dispara el muro**, no la velocidad de navegación.
No merece la pena repetir la prueba de "ir despacio" — no es la causa. Sin
avance, nada insertado. Mismo siguiente objetivo: Recuevas → "Placas".

**2026-08-14 (tanda siguiente, bloqueo idéntico)**: reprobado de nuevo,
`thecrag.com` sigue "blocked by policy". Sin avance, nada insertado. Mismo
siguiente objetivo: Recuevas → "Placas".

**2026-08-14 (tanda siguiente, bloqueo sin cambios)**: reprobado una vez
más, `thecrag.com` sigue "blocked by policy". Sin avance, nada insertado.
Mismo siguiente objetivo: Recuevas → "Placas".

## Pendientes con MUY POCO (1-3 elementos) — revisar antes que las vacías

| id | Nombre | Estilo | Región | Elementos |
|---|---|---|---|---|
| ahedo | Ahedo | Bloque |  Burgos | 2 |
| alcaniz | Alcañiz | Bloque | Aragón | 1 |
| aguero | Agüero | Vía | Aragón | 2 |
| cabo-negro | Cabo Negro | Bloque | Asturias | 2 |
| el-burguillo | El Burguillo | Bloque | Castilla y León | 3 |
| rozas | Rozas | Bloque | Castilla y León | 3 |
| torrelodones | Torrelodones | Bloque | Comunidad de Madrid | 1 |
| pena-pintada | Peña pintada | Vía | Comunidad de Madrid | 1 |
| el-vellon | El Vellón | Vía | Comunidad de Madrid | 3 |
| patones-pueblo | Patones Pueblo | Vía | Comunidad de Madrid | 1 |
| ponton-de-la-oliva | Pontón de la Oliva | Vía | Comunidad de Madrid | 1 |
| caldas-eirizgr | Caldas - Eirizgr | Bloque | Galicia | 3 |

## Pendientes VACÍAS, agrupadas por región (orden geográfico sugerido)

###  Jaén (1)

| id | Nombre | Estilo | Localización | lat,lon |
|---|---|---|---|---|
| pegalajar | Pegalajar | Bloque |  Pegalajar | 37.73,-3.66 |

### Andalucía (21)

| id | Nombre | Estilo | Localización | lat,lon |
|---|---|---|---|---|
| bujeo-el-tarifa | Bujeo, El (Tarifa) | Bloque | Tarifa | 36.078,-5.615 |
| canon-de-manilva-manilva | Cañón de Manilva (Manilva) | Vía | Manilva | 36.37,-5.248 |
| poyatos-algatocin | Poyatos (Algatocín) | Vía | Algatocín | 36.568,-5.272 |
| mijas-mijas | Mijas (Mijas) | Vía | Mijas | 36.596,-4.685 |
| ubrique | Ubrique | Vía | Ubrique | 36.681,-5.447 |
| ronda | Ronda | Vía | Ronda | 36.742,-5.17 |
| frigiliana | Frigiliana | Vía | Frigiliana | 36.788,-3.899 |
| velez-de-benaudalla-velez | Velez de Benaudalla (Velez) | Vía | Vélez de Benaudalla | 36.834,-3.521 |
| salto-del-gallo-almeria | Salto del Gallo (Almería) | Vía | Almería | 36.838,-2.468 |
| el-chorro | El Chorro | Vía | Ardales | 36.908,-4.761 |
| el-torcal-de-antequera | El Torcal de Antequera | Vía | Antequera | 36.955,-4.547 |
| barranco-el-fuerte-illar | Barranco El Fuerte (Illar) | Vía | Íllar | 36.957,-2.636 |
| villanueva-del-rosario | Villanueva del Rosario | Vía | Villanueva del Rosario | 36.988,-4.377 |
| alhama-de-granada | Alhama de Granada | Vía | Alhama de Granada | 37.014,-3.988 |
| archidona | Archidona | Vía | Archidona | 37.094,-4.388 |
| los-cahorros | Los Cahorros | Vía | Monachil | 37.128,-3.55 |
| loja | Loja | Vía | Loja | 37.171,-4.164 |
| moclin | Moclín | Vía | Moclín | 37.331,-3.786 |
| reguchillo | Reguchillo | Vía | Cordel de Jabalcuz, Jaén | 37.750494,-3.826632 |
| recuchillo-jaen | Recuchillo (jaén) | Vía | Jaén | 37.776,-3.789 |
| cazorla | Cazorla | Vía | Cazorla | 37.91,-2.965 |

### Aragón (21)

| id | Nombre | Estilo | Localización | lat,lon |
|---|---|---|---|---|
| bezas | Bezas | Bloque |  Provincia de Teruel, Bezas | 40.33,-1.33 |
| pitarejo-pitarque | Pitarejo (Pitarque) | Vía | Pitarque | 40.627,-0.57 |
| huesa-del-coman-huesa-del-coman | Huesa del Coman (Huesa del Coman) | Vía | Huesa del Común | 40.919,-1.097 |
| hoz-de-la-vieja | Hoz de la Vieja | Vía | Hoz de la Vieja | 40.995,-0.816 |
| jaraba-jaraba | Jaraba (Jaraba) | Vía | Jaraba | 41.18,-1.899 |
| nuevalos | Nuévalos | Vía | Nuévalos | 41.208,-1.787 |
| morata-de-jalon | Morata de Jalón | Vía | Morata de Jalón | 41.455,-1.459 |
| calcena-calcena | Calcena (Calcena) | Vía | Calcena | 41.653,-1.72 |
| alquezar-alquezar | Alquézar (Alquézar) | Vía | Alquézar | 42.173,0.083 |
| mallos-de-vadiello | Mallos de Vadiello | Vía | Balneario de Panticosa | 42.175,-0.175 |
| rodellar | Rodellar | Vía | Rodellar, Bierge (Huesca) | 42.286612,-0.081925 |
| valle-de-isabena-beranuy-y-merli | Valle de Isábena (Beranuy y Merli) | Vía | Beranuy y Merli | 42.323,0.498 |
| mallos-de-riglos | Mallos de Riglos | Vía | Las Peñas de Riglos | 42.339,-0.738 |
| liguerre-de-cinca-ainsa | Ligüerre de Cinca (Ainsa) | Vía | Aínsa | 42.367,0.174 |
| remune-benasque | Remuñe (Benasque) | Vía | Benasque | 42.604,0.513 |
| bielsa | Bielsa | Vía | Bielsa | 42.632,0.221 |
| ordesa-torla | Ordesa (Torla) | Vía | Torla / Ordesa | 42.655,-0.093 |
| villanua | Villanúa | Vía | Villanúa | 42.685,-0.548 |
| panticosa-panticosa | Panticosa (Panticosa) | Vía | Panticosa | 42.726,-0.262 |
| valle-de-hecho | Valle de Hecho | Vía | Hecho | 42.741,-0.751 |
| anso | Ansó | Vía | Ansó | 42.758,-0.819 |

### Aragón / Cataluña (1)

| id | Nombre | Estilo | Localización | lat,lon |
|---|---|---|---|---|
| pared-de-catalunya-montrebei-pont-de-montanyana | Pared de Catalunya-Montrebei (Pont de Montanyana) | Vía | Pont de Montanyana / Montrebei | 42.093,0.68 |

### Asturias (6)

| id | Nombre | Estilo | Localización | lat,lon |
|---|---|---|---|---|
| quiros-aciera | Quirós (Aciera) | Vía | Aciera / Quirós | 43.178,-5.949 |
| naranjo-de-bulnes | Naranjo de Bulnes | Vía | Arenas de Cabrales | 43.198,-4.85 |
| proaza | Proaza | Vía | Proaza | 43.275,-6.012 |
| onis | Onís | Vía | Onís | 43.298,-5.039 |
| coallafu-coallafu | Coallafu (Coallafu) | Vía | Coalla / Grado | 43.393,-6.07 |
| la-nora | La Ñora | Bloque | Gijón, San Clemente | 43.547996,-5.592842 |

### Asturias / Cantabria / Leon (1)

| id | Nombre | Estilo | Localización | lat,lon |
|---|---|---|---|---|
| picos-de-europa | Picos de Europa | Vía | Picos de Europa | 43.173,-4.847 |

### Canarias (6)

| id | Nombre | Estilo | Localización | lat,lon |
|---|---|---|---|---|
| mogan | Mogán | Bloque | Mogán | 27.884,-15.722 |
| roque-nublo-ayacata-gran-canaria | Roque Nublo (Ayacata. Gran Canaria) | Vía | Ayacata, Gran Canaria | 27.969,-15.604 |
| tamadaba | Tamadaba | Vía | Gran Canaria | 28.06,-15.712 |
| arico-nuevo | Arico Nuevo | Bloque | Tenerife, Arico | 28.175971,-16.480345 |
| barranco-la-madera-santa-cruz-de-la-palma | Barranco la Madera (Santa Cruz de la Palma) | Vía | Santa Cruz de La Palma | 28.683,-17.764 |
| jameo-de-la-puerta-falsa-haria-lanzarote | Jameo de la Puerta Falsa (Haría. Lanzarote) | Vía | Haría, Lanzarote | 29.136,-13.508 |

### Cantabria (3)

| id | Nombre | Estilo | Localización | lat,lon |
|---|---|---|---|---|
| ramales-de-la-victoria | Ramales de la Victoria | Vía | Ramales de la Victoria | 43.258,-3.454 |
| pechon | Pechon | Vía |  Pechón | 43.39,-4.47 |
| los-cantosca | Los Cantos | Vía | Suances | 43.418223,-4.033157 |

### Castilla y León (19)

| id | Nombre | Estilo | Localización | lat,lon |
|---|---|---|---|---|
| arenas-de-san-pedro | Arenas de San Pedro | Vía | Arenas de San Pedro | 40.21,-5.092 |
| circo-de-gredos-hoyos-del-espino | Circo de Gredos (Hoyos del Espino) | Vía | Hoyos del Espino | 40.298,-5.173 |
| dehesa-de-candelario | Dehesa de Candelario | Bloque | Candelario | 40.32,-5.76 |
| venteadero-o-ventorro-de-pelayo-bejar | Venteadero o Ventorro de Pelayo (Béjar) | Vía | Béjar | 40.382,-5.763 |
| navalosa | Navalosa | Bloque | Provincia de Ávila, Navalosa | 40.400645,-4.924836 |
| becedas | Becedas | Bloque | Provincia de Ávila, Becedas | 40.409657,-5.64146 |
| valdesangil | Valdesangil | Bloque | Valdesangil | 40.428,-5.674 |
| el-barraco | El Barraco | Vía | El Barraco | 40.437,-4.712 |
| hoces-del-duraton | Hoces del Duratón | Vía | Sepúlveda | 41.239,-3.814 |
| muelas-las-muelas-del-pan | Muelas, Las (Muelas del Pan) | Vía | Muelas del Pan | 41.59,-5.833 |
| laguna-negra-vinuesa | Laguna Negra (Vinuesa) | Vía | Vinuesa | 41.958,-2.752 |
| fragas-del-sil-ponferrada | Fragas del Sil (Ponferrada) | Vía | Ponferrada | 42.436,-6.723 |
| san-martin-de-ubierna-san-martin-de-ubierna | San Martín de Ubierna (San Martín de Ubierna) | Vía | San Martín de Ubierna | 42.499,-3.636 |
| pancorbo | Pancorbo | Vía | Pancorbo | 42.627,-3.108 |
| pena-amaya | Peña Amaya | Vía | Merindad de Castilla Vieja | 42.643,-4.166 |
| sedano | Sedano | Vía | Sedano | 42.72,-3.743 |
| la-boyeriza-geras-de-gordon | La Boyeriza (Geras de Gordón) | Vía | Geras de Gordón | 42.836,-5.664 |
| hoces-de-vegacervera-vegacervera | Hoces de Vegacervera (Vegacervera) | Vía | Vegacervera | 42.893,-5.536 |
| valle-de-valdeon-cain | Valle de Valdeón (Caín) | Bloque | Posada de Valdeón | 43.1701,4.9033 |

### Castilla-La Mancha (3)

| id | Nombre | Estilo | Localización | lat,lon |
|---|---|---|---|---|
| cuenca | Cuenca | Vía | Cuenca, Ciudad de Cuenca | 40.087134,-2.125254 |
| castillo-de-bayuela | Castillo de Bayuela | Vía | Provincia de Toledo, Castillo de Bayuela | 40.108353,-4.684644 |
| congosto-de-alcorlo-san-andres-de-congosto | Congosto de Alcorlo (San Andrés de Congosto) | Vía | San Andrés de Congosto | 41.049,-2.977 |

### Cataluña (31)

| id | Nombre | Estilo | Localización | lat,lon |
|---|---|---|---|---|
| beceite | Beceite | Vía | Beceite | 40.827,0.188 |
| siurana-siurana-de-prades | Siurana (Siurana de Prades) | Vía | Siurana de Prades | 41.259,0.925 |
| mont-ral-mont-ral | Mont-Ral (Mont-Ral) | Vía | Mont-ral | 41.296,1.089 |
| margalef | Margalef | Vía | Margalef de Montsant (Tarragona) | 41.297761,0.776489 |
| montsant | Montsant | Vía | Ulldemolins | 41.312,0.853 |
| pontons-pontons | Pontons (Pontons) | Vía | Pontons | 41.385,1.553 |
| fuixarda-la-barcelona | Fuixarda, La (Barcelona) | Vía | Barcelona | 41.407,2.17 |
| pic-de-l-aliga-sant-andreu-de-la-barca | Pic de l’Aliga (Sant Andreu de la Barca) | Vía | Sant Andreu de la Barca | 41.451,1.969 |
| el-cogul | El Cogul | Bloque | El Cogul (Lleida) | 41.46501,0.697195 |
| cadira-del-bisbe-la-premia-de-dalt | Cadira del Bisbe, La (Premiá de Dalt) | Vía | Premià de Dalt | 41.497,2.34 |
| can-boquet | Can Boquet | Bloque | Vilassar de Dalt / colinas de Barcelona | 41.511418,2.337474 |
| montserrat | Montserrat | Vía | Montserrat | 41.594,1.835 |
| odena | Ódena | Bloque | Igualada | 41.599,1.634 |
| sant-llorenc-del-munt | Sant Llorenç del Munt | Vía | Sant Llorenç del Munt | 41.645,2.008 |
| corona-la-y-la-trona-centelles | Corona, La y La Trona (Centelles) | Vía | Centelles | 41.766,2.22 |
| alos-de-balaguer-alos-de-balaguer | Alos de Balaguer (Alos de Balaguer) | Vía | Alòs de Balaguer | 41.856,0.944 |
| camarasa | Camarasa | Vía | Camarasa | 41.871,0.878 |
| santa-linya | Santa Linya | Vía | Santa Linya, Balaguer (Lleida) | 41.925998,0.81187 |
| savassona | Savassona | Bloque | Tavèrnoles | 41.961,2.336 |
| vilanova-de-meia | Vilanova de Meià | Vía | Vilanova de Meià | 41.969,1.025 |
| la-comarca | La Comarca | Bloque | Provincia de Barcelona, Les Masies de Roda | 41.979547,2.343454 |
| tavertet | Tavertet | Vía | Tavertet | 41.997,2.418 |
| terradets | Terradets | Vía | Cellers | 42.047,0.867 |
| oliana-contrafot-de-rumbau | Oliana (Contrafot de Rumbau) | Vía | Oliana | 42.07,1.314 |
| berga-berga | Berga (Berga) | Vía | Berga | 42.101,1.846 |
| beuda-beuda | Beuda (Beuda) | Vía | Beuda | 42.211,2.69 |
| tres-ponts | Tres Ponts | Vía | Coll de Nargó | 42.223,1.329 |
| pedraforca-saldes | Pedraforca (Saldes) | Vía | Saldes | 42.244,1.704 |
| collegats | Collegats | Vía | La Pobla de Segur | 42.281,0.955 |
| escales-pont-de-suert | Escales (Pont de Suert) | Vía | El Pont de Suert | 42.355,0.744 |
| la-jonquera-puig-del-corb | La Jonquera (Puig del Corb) | Bloque | Girona, La Jonquera / Cantallops | 42.424064,2.903636 |

### Comunidad Valenciana (17)

| id | Nombre | Estilo | Localización | lat,lon |
|---|---|---|---|---|
| rincon-de-bonanza-orihuela | Rincón de Bonanza (Orihuela) | Vía | Orihuela | 38.069,-0.987 |
| pared-negra-frontal-de-orihuela-orihuela | Pared Negra. Frontal de Orihuela (Orihuela) | Vía | Orihuela | 38.084,-0.942 |
| crevillent | Crevillent | Bloque | Crevillent | 38.249,-0.809 |
| el-bancal | El Bancal | Bloque | Crevillent, Alicante | 38.27314,-0.807597 |
| forada-petrer | Foradá (Petrer) | Vía | Petrer | 38.493,-0.775 |
| sella | Sella | Vía | Sella | 38.622,-0.297 |
| calpe | Calpe | Vía | Calpe | 38.641,0.075 |
| guadalest | Guadalest | Vía | Guadalest | 38.679,-0.207 |
| uxola-alcoy | Uxola (Alcoy) | Vía | Alcoy | 38.696,-0.475 |
| gandia | Gandía | Vía | Gandía | 38.931,-0.261 |
| bunol | Buñol | Vía | Buñol | 39.422,-0.794 |
| chulilla | Chulilla | Vía | Chulilla (Valencia) | 39.652956,-0.899559 |
| alfondeguilla | Alfondeguilla | Vía | Alfondeguilla | 39.806,-0.245 |
| tales-onda | Tales (Onda) | Vía | Onda / Tales | 39.958,-0.264 |
| castellet-castellon-de-la-plana | Castellet (Castellón de la Plana) | Vía | Castellón de la Plana | 39.986,-0.049 |
| montanejos | Montanejos | Vía | Montanejos | 39.998,-0.502 |
| vilafames | Vilafamés | Bloque | Vilafamés (Castellón) | 40.106415,-0.05299 |

### Comunidad de Madrid (5)

| id | Nombre | Estilo | Localización | lat,lon |
|---|---|---|---|---|
| colmenar-viejo | Colmenar Viejo | Vía | Colmenar Viejo | 40.668,-3.779 |
| penarrubia | Peñarrubia | Vía | Guadalix de la Sierra (Madrid) | 40.771606,-3.664497 |
| torrelaguna | Torrelaguna | Vía | Torrelaguna | 40.831,-3.542 |
| bustarviejo | Bustarviejo | Bloque | Bustarviejo | 40.845887,-3.685031 |
| valdemanco | Valdemanco | Vía | Valdemanco | 40.874,-3.671 |

### Extremadura (6)

| id | Nombre | Estilo | Localización | lat,lon |
|---|---|---|---|---|
| puerto-roque-valencia-de-alcantara | Puerto Roque (Valencia de Alcántara) | Vía | Valencia de Alcántara | 39.413,-7.237 |
| cabanas-del-castillo-cabanas-del-castillo | Cabañas del Castillo (Cabañas del Castillo) | Vía | Cabañas del Castillo | 39.449,-5.558 |
| los-barruecos | Los Barruecos | Bloque | Cáceres | 39.551,-6.451 |
| monfrague | Monfragüe | Vía | Plasencia | 39.848,-6.024 |
| valcorchero | Valcorchero | Bloque | Monte Valcorchero, Plasencia | 40.054049,-6.095684 |
| valle-del-jerte | Valle del Jerte | Vía | Jerte | 40.176,-5.805 |

### Galicia (10)

| id | Nombre | Estilo | Localización | lat,lon |
|---|---|---|---|---|
| monte-galineiro-vincios | Monte Galiñeiro (Vincios) | Vía | Vincios | 42.234,-8.667 |
| peton-do-xalo-castelo | Petón do Xalo (Castelo) | Vía | Castelo | 42.333,-8.217 |
| pena-corneira | Pena Corneira | Bloque | Provincia de Ourense, Leiro | 42.370711,-8.179021 |
| pena-do-encanto | Pena do Encanto | Vía | Leiro | 42.445,-7.815 |
| segad-caldas-de-reis | Segad (Caldas de Reis) | Vía | Caldas de Reis | 42.605,-8.642 |
| castro-de-barona-barona-porto-do-son | Castro de Baroña (Baroña, Porto do son) | Vía | Baroña, Porto do Son | 42.694,-9.025 |
| pico-sacro | Pico Sacro | Vía | Boqueixón | 42.819,-8.388 |
| corme | Corme | Bloque | O Porto de Corme, Ponteceso (A Coruña) | 43.288815,-8.941101 |
| punta-nariga | Punta Nariga | Bloque | A Coruña, Mens | 43.320463,-8.909885 |
| presa-do-eume-goente | Presa do Eume (Goente) | Vía | Goente | 43.396,-8.098 |

### Islas Baleares (6)

| id | Nombre | Estilo | Localización | lat,lon |
|---|---|---|---|---|
| castell-de-santueri-celanita | Castell de Santueri (Celanita) | Vía | Felanitx / Castell de Santueri | 39.447,3.127 |
| es-verger-esporles | Es Verger (Esporles) | Vía | Esporles | 39.672,2.573 |
| fraguels-bunyola | Fraguels (Bunyola) | Vía | Bunyola | 39.69,2.7 |
| sa-gubia | Sa Gubia | Vía | Bunyola | 39.7,2.685 |
| cales-coves-sant-climent-menorca | Cales Coves (Sant Climent, Menorca) | Vía | Sant Climent, Menorca | 39.873,4.127 |
| formentor | Formentor | Vía | Formentor | 39.956,3.179 |

### Navarra (7)

| id | Nombre | Estilo | Localización | lat,lon |
|---|---|---|---|---|
| lumbier | Lumbier | Vía | Lumbier | 42.647,-1.3 |
| larraona | Larraona | Bloque | Larraona (Navarra) | 42.779839,-2.242085 |
| larraona-bloque | Larraona Bloque | Bloque | Larraona | 42.78,-2.242 |
| etxauri | Etxauri | Vía | Etxauri | 42.783,-1.78 |
| etxauri-bloque | Etxauri Bloque | Bloque | Etxauri | 42.783,-1.78 |
| urbasa | Urbasa | Vía | Urbasa | 42.861,-2.133 |
| dos-hermanas-biaizpe-irurtzun | Dos Hermanas-Biaizpe (Irurtzun) | Vía | Irurtzun | 42.899,-1.819 |

### País Vasco (7)

| id | Nombre | Estilo | Localización | lat,lon |
|---|---|---|---|---|
| eguino | Eguino | Bloque | Arrasate | 42.903,-2.261 |
| araotz | Araotz | Vía | Oñati | 43.009,-2.385 |
| baltzola | Baltzola | Bloque | Galdakao | 43.023,-2.624 |
| atxarte | Atxarte | Vía | Atxarte | 43.097,-2.613 |
| kantera-azpeitia | Kantera (Azpeitia) | Vía | Azpeitia | 43.183,-2.267 |
| jaizkibel | Jaizkibel | Bloque | Gipuzkoa, Jaizkibel | 43.364841,-1.839355 |
| ogono | Ogoño | Vía | Ibarrangelu | 43.405,-2.647 |

### Región de Murcia (4)

| id | Nombre | Estilo | Localización | lat,lon |
|---|---|---|---|---|
| leiva | Leiva | Vía | Alhama de Murcia | 37.874,-1.516 |
| la-panocha | La Panocha | Vía | Murcia | 37.964,-1.125 |
| almorchon-el-cieza | Almorchón, El (Cieza) | Vía | Cieza | 38.238,-1.421 |
| buey-el-jumilla | Buey, El (Jumilla) | Vía | Jumilla | 38.477,-1.326 |

## Dudas para Rodrigo

- **las-tuerces**: la coordenada de la escuela en la BD (`42.792,-4.255`) discrepa
  ~4 km de la que da TheCrag como punto de acceso (`42.754,-4.255`, ya usada para
  el parking insertado). Puede ser que el punto de la BD sea el pueblo y el de
  TheCrag la zona de escalada real. Decidir si se corrige el punto de la escuela.

