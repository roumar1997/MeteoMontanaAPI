-- Caché persistente del forecast CRUDO de Open-Meteo, rellenada cada hora por
-- ForecastPrefetchScheduler (y al arrancar). Sobrevive a los redeploys —a
-- diferencia de la caché en memoria (Caffeine)— así que tras reiniciar Railway
-- no hay pico de cientos de llamadas a Open-Meteo (era lo que disparaba el 429).
CREATE TABLE forecast_cache (
    coord_key  VARCHAR(64) PRIMARY KEY,   -- "lat,lon" (misma clave que la caché en memoria)
    lat        DOUBLE PRECISION NOT NULL,
    lon        DOUBLE PRECISION NOT NULL,
    payload    TEXT NOT NULL,             -- JSON crudo de OpenMeteoResponse
    fetched_at TIMESTAMP NOT NULL
);
