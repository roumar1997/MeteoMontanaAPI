-- Propuestas de escuelas nuevas creadas por usuarios.
-- Cuando un admin las aprueba pasan a la tabla schools.
CREATE TABLE school_submissions (
    id VARCHAR(80) PRIMARY KEY,
    proposed_name VARCHAR(120) NOT NULL,
    proposed_region VARCHAR(80),
    proposed_style VARCHAR(40),
    proposed_rock_type VARCHAR(60),
    proposed_lat DOUBLE PRECISION NOT NULL,
    proposed_lon DOUBLE PRECISION NOT NULL,
    proposed_location VARCHAR(200),
    proposed_source VARCHAR(500),
    notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    submitted_by_uid VARCHAR(255) NOT NULL,
    reviewed_by_uid VARCHAR(255),
    review_reason VARCHAR(500),
    created_school_id VARCHAR(80),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP,
    CONSTRAINT chk_submission_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_submissions_status ON school_submissions (status);
CREATE INDEX idx_submissions_submitted_by ON school_submissions (submitted_by_uid);

-- Log inmutable de acciones admin. Solo INSERT, nunca UPDATE/DELETE.
CREATE TABLE admin_logs (
    id VARCHAR(80) PRIMARY KEY,
    actor_uid VARCHAR(255) NOT NULL,
    action VARCHAR(60) NOT NULL,
    target_type VARCHAR(40) NOT NULL,
    target_id VARCHAR(80) NOT NULL,
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_admin_logs_actor ON admin_logs (actor_uid);
CREATE INDEX idx_admin_logs_created_at ON admin_logs (created_at DESC);
