-- Filtros avanzados para la alerta de quedadas: disciplina, privacidad,
-- distancia (con ubicación guardada al activar la alerta "por cercanía").
ALTER TABLE meetup_alerts ADD COLUMN IF NOT EXISTS discipline VARCHAR(16);
ALTER TABLE meetup_alerts ADD COLUMN IF NOT EXISTS privacy VARCHAR(16);
ALTER TABLE meetup_alerts ADD COLUMN IF NOT EXISTS max_distance_km INT;
ALTER TABLE meetup_alerts ADD COLUMN IF NOT EXISTS user_lat DOUBLE PRECISION;
ALTER TABLE meetup_alerts ADD COLUMN IF NOT EXISTS user_lon DOUBLE PRECISION;
