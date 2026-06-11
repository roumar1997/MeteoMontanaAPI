-- Fix validacion Hibernate (ddl-auto: validate): la entidad usa int (INTEGER),
-- V18 creo las columnas como SMALLINT y el arranque fallaba.
ALTER TABLE weekend_alert_prefs ALTER COLUMN notify_day TYPE INTEGER;
ALTER TABLE weekend_alert_prefs ALTER COLUMN notify_hour TYPE INTEGER;
