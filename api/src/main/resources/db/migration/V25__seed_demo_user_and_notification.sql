-- Seed de PRUEBAS (solicitado por el dueño): un perfil público falso con foto y
-- una notificación in-app para la cuenta de Álvaro, para validar en iOS que se
-- puede ver un perfil público ajeno y que llega una notificación a la bandeja.
-- Reversible con una migración posterior que borre estas dos filas por id.

-- 1) Usuario público de demostración (foto = avatar público estable).
INSERT INTO users (uid, email, username, display_name, photo_path, bio,
                   is_public, is_admin, is_premium, created_at, updated_at)
VALUES ('demo-cumbre-001', 'demo@cumbre.app', 'cumbre_demo', 'Cumbre Demo',
        'https://i.pravatar.cc/300?img=8',
        'Perfil de demostración para probar la app. Escalador de prueba.',
        TRUE, FALSE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (uid) DO NOTHING;

-- 2) Notificación "nuevo seguidor" para Álvaro (se resuelve su uid por email).
--    Al tocarla, la app abre el perfil público del usuario demo (target_type=user).
INSERT INTO notifications (id, uid, type, title, body, target_type, target_id, read_at, created_at)
SELECT 'demo-notif-001', u.uid, 'NEW_FOLLOWER', 'Nuevo seguidor',
       'Cumbre Demo ha empezado a seguirte.', 'user', 'demo-cumbre-001', NULL, CURRENT_TIMESTAMP
FROM users u
WHERE LOWER(u.email) = 'roumar1997@gmail.com'
ON CONFLICT (id) DO NOTHING;
