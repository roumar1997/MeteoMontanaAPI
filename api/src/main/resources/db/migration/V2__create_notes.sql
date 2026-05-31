CREATE TABLE notes (
                       id VARCHAR(80) PRIMARY KEY,
                       school_id VARCHAR(80) NOT NULL,
                       text TEXT NOT NULL,
                       author VARCHAR(100) NOT NULL,
                       uid VARCHAR(255) NOT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       upvotes_count INT DEFAULT 0,
                       downvotes_count INT DEFAULT 0,
                       CONSTRAINT fk_notes_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE CASCADE
);

CREATE INDEX idx_notes_school_id ON notes (school_id);
CREATE INDEX idx_notes_uid ON notes (uid);