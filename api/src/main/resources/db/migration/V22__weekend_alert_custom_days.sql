-- Alerta de tiempo: el usuario elige qué días de la semana comparar
-- (CSV de días ISO-8601, 1=lunes .. 7=domingo). Antes era fijo vie/sáb/dom.
ALTER TABLE weekend_alert_prefs
    ADD COLUMN alert_days VARCHAR(20) NOT NULL DEFAULT '5,6,7';
