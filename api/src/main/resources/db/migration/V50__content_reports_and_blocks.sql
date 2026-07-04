-- Moderación de contenido de usuarios (requisito App Store 1.2 para apps con
-- UGC): denuncias de comentarios/notas/usuarios + bloqueo entre usuarios.

CREATE TABLE content_reports (
    id            VARCHAR(80)  PRIMARY KEY,
    reporter_uid  VARCHAR(255) NOT NULL,
    -- COMMENT (line_comments) / NOTE (notes) / USER (users)
    target_type   VARCHAR(20)  NOT NULL,
    target_id     VARCHAR(255) NOT NULL,
    -- SPAM / OFFENSIVE / FALSE_INFO / OTHER
    reason        VARCHAR(30)  NOT NULL,
    -- Copia del texto/autor denunciado: el admin puede juzgar aunque el
    -- contenido original se borre después.
    snapshot      VARCHAR(1200),
    author_uid    VARCHAR(255),
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    resolution    VARCHAR(20),
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at   TIMESTAMP
);
CREATE INDEX idx_content_reports_status ON content_reports (status);

-- Bloqueo: el bloqueador deja de ver contenido del bloqueado y este no puede
-- iniciarle chat.
CREATE TABLE user_blocks (
    blocker_uid VARCHAR(255) NOT NULL,
    blocked_uid VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (blocker_uid, blocked_uid)
);
CREATE INDEX idx_user_blocks_blocker ON user_blocks (blocker_uid);
