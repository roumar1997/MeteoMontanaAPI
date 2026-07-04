-- Comentarios de la comunidad en piedras/muros y en vías concretas, con votos
-- de utilidad (mismo modelo que notes + note_votes). target: un bloque entero
-- (line_id NULL) o una vía concreta (line_id = block_lines.id).
-- OJO tipos: INT (no SMALLINT) para casar con los int de JPA (lección V46/V47).
CREATE TABLE line_comments (
    id              VARCHAR(80)  PRIMARY KEY,
    block_id        VARCHAR(80)  NOT NULL,
    line_id         VARCHAR(80),
    uid             VARCHAR(255) NOT NULL,
    author          VARCHAR(120) NOT NULL,
    text            VARCHAR(1000) NOT NULL,
    upvotes_count   INT NOT NULL DEFAULT 0,
    downvotes_count INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_line_comments_block FOREIGN KEY (block_id)
        REFERENCES school_blocks(id) ON DELETE CASCADE
);

CREATE INDEX idx_line_comments_block ON line_comments (block_id);
CREATE INDEX idx_line_comments_line ON line_comments (line_id);

CREATE TABLE line_comment_votes (
    id         VARCHAR(160) PRIMARY KEY,   -- commentId:uid
    comment_id VARCHAR(80)  NOT NULL,
    uid        VARCHAR(255) NOT NULL,
    vote_value INT          NOT NULL,      -- 1 = me gusta, -1 = no me gusta
    CONSTRAINT fk_lc_votes_comment FOREIGN KEY (comment_id)
        REFERENCES line_comments(id) ON DELETE CASCADE,
    CONSTRAINT uq_lc_votes UNIQUE (comment_id, uid)
);

CREATE INDEX idx_lc_votes_comment ON line_comment_votes (comment_id);

-- Descripción opcional de una vía (beta, salida, detalle del bloque…): se
-- rellena al crearla/editarla y se muestra en su ficha.
ALTER TABLE block_lines ADD COLUMN description VARCHAR(500);
