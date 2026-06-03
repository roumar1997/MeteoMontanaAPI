-- Sesiones de escalada del diario personal.
-- Cada fila = un "bloque" que el usuario ha encadenado o intentado.
CREATE TABLE journal_sessions (
    id VARCHAR(80) PRIMARY KEY,
    uid VARCHAR(255) NOT NULL,
    school_id VARCHAR(80),                  -- puede ser null (texto libre)
    school_name VARCHAR(120),               -- denormalizado para evitar JOIN al listar
    sector VARCHAR(120),                    -- opcional
    block_name VARCHAR(160) NOT NULL,       -- nombre del bloque/vía
    grade VARCHAR(8),                       -- 7a, 7b+, 8a, etc
    notes VARCHAR(500),                     -- libres
    session_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_journal_uid ON journal_sessions (uid);
CREATE INDEX idx_journal_school ON journal_sessions (school_id);
CREATE INDEX idx_journal_date ON journal_sessions (session_date DESC);
