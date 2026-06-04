-- Bloques/piedras/parkings/zonas dibujados sobre el mapa de cada escuela.
-- Type: BLOCK | PARKING | ZONE
CREATE TABLE school_blocks (
    id VARCHAR(80) PRIMARY KEY,
    school_id VARCHAR(80) NOT NULL,
    type VARCHAR(20) NOT NULL,           -- BLOCK / PARKING / ZONE
    name VARCHAR(160) NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lon DOUBLE PRECISION NOT NULL,
    photo_path VARCHAR(500),             -- foto en storage (solo BLOCK normalmente)
    description VARCHAR(500),
    created_by_uid VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_school_blocks_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE CASCADE,
    CONSTRAINT chk_block_type CHECK (type IN ('BLOCK', 'PARKING', 'ZONE'))
);

CREATE INDEX idx_school_blocks_school ON school_blocks (school_id);
CREATE INDEX idx_school_blocks_type ON school_blocks (type);

-- Líneas dentro de un bloque (rutas de escalada).
-- Cada línea: nombre, grado, tipo de inicio (SIT, STAND, JUMP, TRAV), color (derivado del grado).
CREATE TABLE block_lines (
    id VARCHAR(80) PRIMARY KEY,
    block_id VARCHAR(80) NOT NULL,
    name VARCHAR(160) NOT NULL,
    grade VARCHAR(8),
    start_type VARCHAR(10),              -- SIT, STAND, JUMP, TRAV
    line_path TEXT,                       -- JSON con array de puntos {x, y} sobre la foto
    sort_order INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_block_lines_block FOREIGN KEY (block_id) REFERENCES school_blocks(id) ON DELETE CASCADE,
    CONSTRAINT chk_start_type CHECK (start_type IS NULL OR start_type IN ('SIT', 'STAND', 'JUMP', 'TRAV'))
);

CREATE INDEX idx_block_lines_block ON block_lines (block_id);
