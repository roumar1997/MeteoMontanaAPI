-- Quedadas para escalar (Fase 1)

-- Género privado del usuario (NUNCA en PublicProfileDto)
ALTER TABLE users ADD COLUMN gender VARCHAR(16) NULL;

-- BOTH como valor válido para discipline en bloques y contribuciones
-- (ya es VARCHAR sin CHECK constraint, así que no hace falta ALTER)

-- Tabla principal de quedadas
CREATE TABLE meetups (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       VARCHAR(36) NOT NULL REFERENCES schools(id),
    name            VARCHAR(80)  NOT NULL,
    discipline      VARCHAR(16)  NULL,      -- BOULDER | ROUTE | BOTH | NULL = sin preferencia
    privacy         VARCHAR(16)  NOT NULL DEFAULT 'OPEN',  -- OPEN | FOLLOWERS | WOMEN
    member_limit    INT          NULL,      -- NULL = sin tope
    photo_url       VARCHAR      NULL,
    creator_uid     VARCHAR      NOT NULL,
    conversation_id VARCHAR      NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    last_day        DATE         NOT NULL,
    expires_at      TIMESTAMP    NOT NULL
);

-- Días de la quedada (puede ser varios)
CREATE TABLE meetup_days (
    meetup_id UUID NOT NULL REFERENCES meetups(id) ON DELETE CASCADE,
    day       DATE NOT NULL,
    PRIMARY KEY (meetup_id, day)
);

-- Miembros de la quedada
CREATE TABLE meetup_members (
    meetup_id UUID      NOT NULL REFERENCES meetups(id) ON DELETE CASCADE,
    uid       VARCHAR   NOT NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (meetup_id, uid)
);

-- Alertas opt-in: "avísame si crean quedadas nuevas para esta escuela/días"
CREATE TABLE meetup_alerts (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    uid       VARCHAR NOT NULL,
    school_id VARCHAR(36) NULL REFERENCES schools(id) ON DELETE CASCADE,
    days_csv  VARCHAR NULL   -- ISO 1-7 (ej: "5,6,7" = vie+sáb+dom), NULL = cualquier día
);

-- Denuncias de usuarios
CREATE TABLE reports (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reported_uid  VARCHAR      NOT NULL,
    reporter_uid  VARCHAR      NOT NULL,
    reason        VARCHAR(300) NOT NULL,
    context_type  VARCHAR(16)  NOT NULL,   -- MEETUP | CHAT | PROFILE
    context_id    VARCHAR      NULL,       -- meetup_id o conversation_id
    status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING',  -- PENDING | RESOLVED | DISMISSED
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

-- Índices útiles
CREATE INDEX idx_meetups_school    ON meetups(school_id);
CREATE INDEX idx_meetups_expires   ON meetups(expires_at);
CREATE INDEX idx_meetups_creator   ON meetups(creator_uid);
CREATE INDEX idx_meetup_alerts_uid ON meetup_alerts(uid);
CREATE INDEX idx_reports_status    ON reports(status);
