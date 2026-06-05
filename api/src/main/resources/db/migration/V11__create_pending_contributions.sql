-- Propuestas de mejora de escuelas existentes por usuarios.
-- Equivalente a la colección `pending_contributions` de Firestore.
-- Tipos: PARKING, BOULDER, SECTOR, POSITION_CORRECTION.
-- El admin las aprueba o rechaza; el usuario recibe notificación push.

CREATE TABLE pending_contributions (
    id                  VARCHAR(80)  PRIMARY KEY,
    type                VARCHAR(40)  NOT NULL,   -- PARKING | BOULDER | SECTOR | POSITION_CORRECTION
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',

    -- Escuela a la que pertenece la propuesta
    school_id           VARCHAR(80)  NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    school_name         VARCHAR(120) NOT NULL,

    -- Datos de la propuesta
    name                VARCHAR(200),            -- nombre del parking/piedra/sector (opcional)
    lat                 DOUBLE PRECISION NOT NULL,
    lon                 DOUBLE PRECISION NOT NULL,
    notes               TEXT,                    -- notas del usuario
    description         TEXT,                    -- descripción extra (bloques, vías, etc.)

    -- Solo para POSITION_CORRECTION
    proposed_lat        DOUBLE PRECISION,
    proposed_lon        DOUBLE PRECISION,
    correction_reason   TEXT,

    -- Metadata
    submitted_by_uid    VARCHAR(255) NOT NULL,
    submitted_by_name   VARCHAR(255),
    reviewed_by_uid     VARCHAR(255),
    review_reason       VARCHAR(500),
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at         TIMESTAMP,

    CONSTRAINT chk_contribution_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_contribution_type   CHECK (type   IN ('PARKING', 'BOULDER', 'SECTOR', 'POSITION_CORRECTION'))
);

CREATE INDEX idx_contributions_school  ON pending_contributions (school_id);
CREATE INDEX idx_contributions_status  ON pending_contributions (status);
CREATE INDEX idx_contributions_user    ON pending_contributions (submitted_by_uid);
CREATE INDEX idx_contributions_created ON pending_contributions (created_at DESC);
