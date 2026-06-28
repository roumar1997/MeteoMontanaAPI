-- Valoraciones de vías (1–5 estrellas). Un usuario solo puede votar una vez por vía.
-- OJO tipos: block_lines.id es VARCHAR(80) y los IDs se generan en Java
-- (UUID.randomUUID().toString()) → todas las columnas de id van como varchar,
-- no uuid (si no, el FK a block_lines falla y Hibernate validate revienta con
-- String↔uuid). stars como INTEGER para casar con el int de la entidad.
CREATE TABLE line_ratings (
    id         VARCHAR(36)  PRIMARY KEY,
    uid        VARCHAR(128) NOT NULL,
    line_id    VARCHAR(80)  NOT NULL REFERENCES block_lines(id) ON DELETE CASCADE,
    stars      INTEGER      NOT NULL CHECK (stars >= 1 AND stars <= 5),
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    UNIQUE (uid, line_id)
);
CREATE INDEX line_ratings_line_id_idx ON line_ratings (line_id);
CREATE INDEX line_ratings_uid_idx     ON line_ratings (uid);
