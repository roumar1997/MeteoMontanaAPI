-- Registro auditable de acciones de moderación: cada aviso/suspensión/baneo/
-- borrado queda con su MOTIVO y una copia del contenido borrado, para poder
-- justificar o revocar la decisión si luego piden pruebas.
CREATE TABLE moderation_actions (
    id          VARCHAR(36) PRIMARY KEY,
    admin_uid   VARCHAR(128) NOT NULL,
    target_uid  VARCHAR(128),           -- usuario afectado (autor del contenido)
    action      VARCHAR(32)  NOT NULL,  -- WARN/SUSPEND/BAN/UNBAN/DELETE_NOTE/DELETE_COMMENT/DELETE_MEETUP
    reason      TEXT,                   -- motivo escrito por el admin
    snapshot    TEXT,                   -- copia del contenido borrado (pruebas)
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_moderation_actions_target ON moderation_actions (target_uid, created_at DESC);
