-- Tokens de push POR DISPOSITIVO. Hasta ahora users.fcm_token guardaba UNO
-- solo por usuario: iniciar sesión en el móvil B machacaba el token del A y
-- las notificaciones dejaban de llegar al primero (visto con Android+iPhone
-- el 2026-07-03). users.fcm_token se conserva por compatibilidad de lectura.
CREATE TABLE user_devices (
    token      VARCHAR(500) PRIMARY KEY,
    uid        VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_devices_uid ON user_devices (uid);

-- Sembrar con los tokens ya conocidos.
INSERT INTO user_devices (token, uid)
SELECT fcm_token, uid FROM users WHERE fcm_token IS NOT NULL
ON CONFLICT (token) DO NOTHING;
