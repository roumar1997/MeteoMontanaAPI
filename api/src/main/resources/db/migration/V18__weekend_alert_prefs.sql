-- Alerta del finde: el usuario elige hasta 3 escuelas y un dia+hora de aviso.
-- Un job evalua vie/sab/dom de esas escuelas y manda un push comparandolas.
CREATE TABLE weekend_alert_prefs (
    uid VARCHAR(255) PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    -- Dia de la semana del aviso, ISO-8601: 1=lunes .. 7=domingo
    notify_day SMALLINT NOT NULL DEFAULT 4,
    -- Hora local (Europe/Madrid) del aviso, 0-23
    notify_hour SMALLINT NOT NULL DEFAULT 20,
    -- CSV de ids de escuela (max 3), p.ej. "albarracin,pedriza,zarzalejo"
    school_ids VARCHAR(300) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_weekend_alert_when ON weekend_alert_prefs (notify_day, notify_hour) WHERE enabled;
