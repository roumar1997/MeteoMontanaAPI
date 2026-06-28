-- Normalización de meetup_alerts (Fase 6).
-- OJO: la tabla meetup_alerts YA se creó en V31 (sin created_at ni UNIQUE).
-- Esta migración NO la recrea (eso fallaba con "relation already exists");
-- en su lugar la ajusta de forma idempotente para que tenga todas las
-- columnas/constraints que el código espera, exista ya o no.

-- Por si en algún entorno la tabla no existiera (defensa): crearla con la forma final.
CREATE TABLE IF NOT EXISTS meetup_alerts (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    uid        VARCHAR(128) NOT NULL,
    school_id  VARCHAR(36)  REFERENCES schools(id) ON DELETE CASCADE,
    days_csv   VARCHAR(64),
    created_at TIMESTAMP    NOT NULL DEFAULT now()
);

-- Si venía de V31, le falta created_at
ALTER TABLE meetup_alerts ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT now();

-- UNIQUE (uid, school_id): una alerta por (usuario, escuela). Añadir si no existe.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'meetup_alerts_uid_school_id_key'
    ) THEN
        ALTER TABLE meetup_alerts
            ADD CONSTRAINT meetup_alerts_uid_school_id_key UNIQUE (uid, school_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS meetup_alerts_uid_idx       ON meetup_alerts (uid);
CREATE INDEX IF NOT EXISTS meetup_alerts_school_id_idx ON meetup_alerts (school_id);
