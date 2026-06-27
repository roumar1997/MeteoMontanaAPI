-- Tabla de alertas de quedadas (Fase 6)
-- El usuario puede activar una alerta para recibir notificación cuando
-- alguien crea una quedada en una escuela concreta (o en cualquier escuela).
-- school_id NULL = cualquier escuela; days_csv NULL = cualquier día.
CREATE TABLE meetup_alerts (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    uid        VARCHAR(128) NOT NULL,
    school_id  UUID         REFERENCES schools(id) ON DELETE CASCADE,
    days_csv   VARCHAR(64),          -- "1,2,3" ISO day-of-week, NULL = cualquier día
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    UNIQUE (uid, school_id)          -- una alerta por (usuario, escuela)
);

CREATE INDEX meetup_alerts_uid_idx       ON meetup_alerts (uid);
CREATE INDEX meetup_alerts_school_id_idx ON meetup_alerts (school_id);
