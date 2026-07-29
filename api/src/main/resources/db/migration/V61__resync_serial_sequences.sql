-- Resincroniza las secuencias de las tablas con id numérico (BIGSERIAL).
--
-- CAUSA (2026-07-29): el espejo de staging se sembró con \copy, que copia las
-- filas pero NO avanza las secuencias → los INSERT nuevos chocaban con ids ya
-- existentes (unique violation) y "publicar en el feed" fallaba en silencio.
-- En prod este setval es inocuo (deja la secuencia donde ya está).
SELECT setval(pg_get_serial_sequence('feed_posts', 'id'),
              COALESCE((SELECT MAX(id) FROM feed_posts), 1));
SELECT setval(pg_get_serial_sequence('radar_frames', 'id'),
              COALESCE((SELECT MAX(id) FROM radar_frames), 1));
