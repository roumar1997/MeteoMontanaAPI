-- Feed social (pestaña Comunidad): posts de actividad (vía/bloque HECHO,
-- proyecto conseguido, y en el futuro piedras/vías nuevas al aprobarse),
-- con likes y comentarios. Ver FEED_DESIGN.md en el repo Android.
--
-- El diario (journal_sessions) sigue siendo PRIVADO: publicar en el feed es
-- una acción explícita del cliente (opt-out en el diálogo del tick), por eso
-- tabla propia con snapshot y no una vista sobre el diario.

CREATE TABLE feed_posts (
    id          BIGSERIAL PRIMARY KEY,
    user_uid    VARCHAR(255) NOT NULL,
    -- snapshots para pintar la tarjeta sin joins (el nombre puede cambiar luego)
    school_id   VARCHAR(80),
    school_name VARCHAR(120),
    block_id    VARCHAR(80) NOT NULL,
    block_name  VARCHAR(160),
    line_id     VARCHAR(80),
    line_name   VARCHAR(160),
    grade       VARCHAR(8),
    -- TICK | PROJECT_DONE (cliente) | NEW_BLOCK | NEW_LINE (los crea el backend
    -- al aprobar una contribución; reservados, aún sin usar)
    kind        VARCHAR(16) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Feed por autor (scope SIGUIENDO) paginado por id descendente.
CREATE INDEX idx_feed_posts_user ON feed_posts (user_uid, id DESC);

-- Anti-spam: un mismo usuario no publica dos veces el mismo ascenso.
CREATE UNIQUE INDEX uq_feed_posts_user_line_kind
    ON feed_posts (user_uid, line_id, kind) WHERE line_id IS NOT NULL;

CREATE TABLE feed_likes (
    post_id    BIGINT NOT NULL REFERENCES feed_posts (id) ON DELETE CASCADE,
    uid        VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (post_id, uid)
);

CREATE TABLE feed_comments (
    id         VARCHAR(80) PRIMARY KEY,
    post_id    BIGINT NOT NULL REFERENCES feed_posts (id) ON DELETE CASCADE,
    uid        VARCHAR(255) NOT NULL,
    author     VARCHAR(120) NOT NULL,
    text       VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_feed_comments_post ON feed_comments (post_id, created_at);
