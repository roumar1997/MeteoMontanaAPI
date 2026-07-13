-- Likes y respuestas en los comentarios del feed (paridad con los posts).
-- parent_id: un nivel de anidación estilo Instagram (responder a una
-- respuesta cuelga del mismo padre). Borrar el padre borra sus respuestas.

ALTER TABLE feed_comments
    ADD COLUMN parent_id VARCHAR(80) REFERENCES feed_comments (id) ON DELETE CASCADE;

CREATE INDEX idx_feed_comments_parent ON feed_comments (parent_id);

CREATE TABLE feed_comment_likes (
    comment_id VARCHAR(80) NOT NULL REFERENCES feed_comments (id) ON DELETE CASCADE,
    uid        VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (comment_id, uid)
);
