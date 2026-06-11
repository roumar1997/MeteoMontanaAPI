-- Modo "por cercania" en la alerta del finde: en vez de escuelas fijas,
-- el job evalua las escuelas en un radio desde la ultima posicion del usuario
-- y compara las 3 mejores.
ALTER TABLE weekend_alert_prefs ADD COLUMN mode VARCHAR(20) NOT NULL DEFAULT 'SCHOOLS';
ALTER TABLE weekend_alert_prefs ADD COLUMN radius_km INTEGER;
ALTER TABLE weekend_alert_prefs ADD COLUMN user_lat DOUBLE PRECISION;
ALTER TABLE weekend_alert_prefs ADD COLUMN user_lon DOUBLE PRECISION;
-- En modo NEARBY school_ids puede ir vacio
ALTER TABLE weekend_alert_prefs ALTER COLUMN school_ids DROP NOT NULL;
