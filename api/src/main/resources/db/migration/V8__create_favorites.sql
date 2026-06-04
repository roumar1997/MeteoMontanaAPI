-- Escuelas favoritas por usuario.
-- Tabla muy simple, sin FK al user (es Firebase uid).
CREATE TABLE favorites (
    uid VARCHAR(255) NOT NULL,
    school_id VARCHAR(80) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (uid, school_id),
    CONSTRAINT fk_favorites_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE CASCADE
);

CREATE INDEX idx_favorites_uid ON favorites (uid);
