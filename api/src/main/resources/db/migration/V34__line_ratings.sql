-- Valoraciones de vías (1–5 estrellas). Un usuario solo puede votar una vez por vía.
CREATE TABLE line_ratings (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    uid        VARCHAR(128) NOT NULL,
    line_id    UUID         NOT NULL REFERENCES block_lines(id) ON DELETE CASCADE,
    stars      SMALLINT     NOT NULL CHECK (stars >= 1 AND stars <= 5),
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    UNIQUE (uid, line_id)
);
CREATE INDEX line_ratings_line_id_idx ON line_ratings (line_id);
CREATE INDEX line_ratings_uid_idx     ON line_ratings (uid);
