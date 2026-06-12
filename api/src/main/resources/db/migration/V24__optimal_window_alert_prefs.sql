-- Alerta "ventana óptima hoy": push cuando una escuela favorita supera un
-- umbral de score en su ventana óptima del día. Convive con la alerta de
-- tiempo en la misma fila de preferencias.
ALTER TABLE weekend_alert_prefs ADD COLUMN optimal_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE weekend_alert_prefs ADD COLUMN optimal_threshold INT NOT NULL DEFAULT 70;
-- Último día (Europe/Madrid) en que se mandó la alerta: máximo un push al día.
ALTER TABLE weekend_alert_prefs ADD COLUMN optimal_last_sent DATE;
