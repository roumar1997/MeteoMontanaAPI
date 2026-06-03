-- FCM token para enviar push al dispositivo del usuario.
-- El cliente envía el token tras login (POST /api/me/fcm-token).
ALTER TABLE users ADD COLUMN fcm_token VARCHAR(500);

-- Índice para encontrar tokens al mandar push masivo.
CREATE INDEX idx_users_fcm_token ON users (fcm_token) WHERE fcm_token IS NOT NULL;
