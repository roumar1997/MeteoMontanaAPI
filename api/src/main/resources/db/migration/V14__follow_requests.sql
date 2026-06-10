-- Sistema de solicitudes de seguimiento para perfiles privados.
-- Status:
--   ACCEPTED -> seguimiento confirmado (cuenta para follower count, ve perfil privado)
--   PENDING  -> solicitud pendiente (no cuenta, no ve perfil)
-- Los existentes pasan a ACCEPTED (no rompemos seguidores actuales).
ALTER TABLE follows ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACCEPTED';

CREATE INDEX idx_follows_status ON follows (followed_uid, status);
