-- Seguidores entre usuarios.
-- (follower_uid sigue a followed_uid)
CREATE TABLE follows (
    follower_uid VARCHAR(255) NOT NULL,
    followed_uid VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (follower_uid, followed_uid),
    CONSTRAINT chk_no_self_follow CHECK (follower_uid <> followed_uid)
);

CREATE INDEX idx_follows_followed ON follows (followed_uid);
CREATE INDEX idx_follows_follower ON follows (follower_uid);

-- Bandeja de notificaciones por usuario.
-- Tipos: NEW_FOLLOWER, SUBMISSION_APPROVED, SUBMISSION_REJECTED, TOP_GRADE_INCREASED,
--         NEW_NOTE_ON_FAVORITE, ADMIN_BROADCAST.
CREATE TABLE notifications (
    id VARCHAR(80) PRIMARY KEY,
    uid VARCHAR(255) NOT NULL,
    type VARCHAR(40) NOT NULL,
    title VARCHAR(160) NOT NULL,
    body VARCHAR(500),
    target_type VARCHAR(40),
    target_id VARCHAR(80),
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_uid_created ON notifications (uid, created_at DESC);
CREATE INDEX idx_notifications_unread ON notifications (uid) WHERE read_at IS NULL;
