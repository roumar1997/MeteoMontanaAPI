-- Votación comunitaria de ORIENTACIÓN (por piedra/sector, y por foto en muros)
-- y de GRADO (por vía). Diseño 2026-07-29 (decisiones de Rodrigo):
--  - Orientación: gana la mayoría, sin admin. photo_index NULL = la piedra/
--    sector entero; en muros cada foto (cara) puede tener la suya.
--  - Grado: hasta 2 votos se muestra el del equipador; con 3+ manda el
--    consenso, y se propaga TAMBIÉN a los diarios (perfil del usuario).
--    setter_grade conserva el grado original del equipador como referencia.

CREATE TABLE block_orientation_votes (
    id          VARCHAR(36) PRIMARY KEY,
    block_id    VARCHAR(36) NOT NULL REFERENCES school_blocks(id) ON DELETE CASCADE,
    photo_index INT,                          -- NULL = bloque/sector entero
    voter_uid   VARCHAR(64) NOT NULL,
    aspect      VARCHAR(2)  NOT NULL,         -- N,NE,E,SE,S,SO,O,NO
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_aspect CHECK (aspect IN ('N','NE','E','SE','S','SO','O','NO'))
);
-- Un voto por persona y superficie (el voto se cambia, no se duplica).
-- UNIQUE con NULL no aplica en Postgres → índice parcial para photo_index NULL.
CREATE UNIQUE INDEX uq_orient_vote_photo
    ON block_orientation_votes (block_id, photo_index, voter_uid)
    WHERE photo_index IS NOT NULL;
CREATE UNIQUE INDEX uq_orient_vote_block
    ON block_orientation_votes (block_id, voter_uid)
    WHERE photo_index IS NULL;
CREATE INDEX idx_orient_votes_block ON block_orientation_votes (block_id);

CREATE TABLE line_grade_votes (
    id         VARCHAR(36) PRIMARY KEY,
    line_id    VARCHAR(36) NOT NULL REFERENCES block_lines(id) ON DELETE CASCADE,
    voter_uid  VARCHAR(64) NOT NULL,
    grade      VARCHAR(8)  NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_grade_vote UNIQUE (line_id, voter_uid)
);
CREATE INDEX idx_grade_votes_line ON line_grade_votes (line_id);

-- El grado original del equipador; block_lines.grade pasa a ser el MOSTRADO
-- (= consenso cuando hay 3+ votos). Backfill: hoy mostrado == original.
ALTER TABLE block_lines ADD COLUMN setter_grade VARCHAR(8);
UPDATE block_lines SET setter_grade = grade;

-- Orientación votada de sectores y piedras: no se persiste el consenso
-- (se calcula al leer los votos), así nunca desincroniza.
