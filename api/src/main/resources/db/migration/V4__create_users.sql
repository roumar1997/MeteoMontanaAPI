CREATE TABLE users (
    uid VARCHAR(255) PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    username VARCHAR(32),
    display_name VARCHAR(120),
    photo_path VARCHAR(500),
    bio VARCHAR(150),
    is_public BOOLEAN NOT NULL DEFAULT TRUE,
    top_grade VARCHAR(8),
    is_admin BOOLEAN NOT NULL DEFAULT FALSE,
    is_premium BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Username único cuando existe (los usuarios pueden no haber elegido aún).
CREATE UNIQUE INDEX idx_users_username ON users (LOWER(username)) WHERE username IS NOT NULL;
CREATE INDEX idx_users_email ON users (email);
