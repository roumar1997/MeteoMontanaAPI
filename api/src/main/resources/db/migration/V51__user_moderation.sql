-- Moderación de usuarios: baneo de login (reversible), suspensión temporal
-- (no puede crear contenido hasta una fecha) y contador de avisos.
ALTER TABLE users ADD COLUMN banned BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN suspended_until TIMESTAMP NULL;
ALTER TABLE users ADD COLUMN warnings INT NOT NULL DEFAULT 0;
