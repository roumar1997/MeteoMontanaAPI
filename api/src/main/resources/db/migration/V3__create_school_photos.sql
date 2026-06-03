CREATE TABLE school_photos (
                               id VARCHAR(80) PRIMARY KEY,
                               school_id VARCHAR(80) NOT NULL,
                               storage_path VARCHAR(500) NOT NULL,
                               uploaded_by_uid VARCHAR(255) NOT NULL,
                               caption VARCHAR(280),
                               width INT,
                               height INT,
                               size_bytes BIGINT,
                               content_type VARCHAR(60),
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               CONSTRAINT fk_school_photos_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE CASCADE
);

CREATE INDEX idx_school_photos_school_id ON school_photos (school_id);
CREATE INDEX idx_school_photos_uploaded_by ON school_photos (uploaded_by_uid);