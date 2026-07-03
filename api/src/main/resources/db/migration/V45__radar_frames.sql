-- Archivo de frames del radar de AEMET.
-- AEMET solo sirve la imagen MÁS RECIENTE de cada radar regional, así que
-- guardamos nosotros el histórico (6h) para poder animar la película de lluvia.
-- Peso: ~10-15 KB por GIF x 15 radares x 36 frames (6h) ≈ 8 MB. Trivial.
CREATE TABLE radar_frames (
    id          BIGSERIAL PRIMARY KEY,
    radar_code  VARCHAR(4)   NOT NULL,   -- 'ma', 'za', 'va'... o 'nac' (composición)
    captured_at TIMESTAMP    NOT NULL,   -- instante de captura (hora del ciclo de descarga)
    image       BYTEA        NOT NULL,   -- GIF crudo de AEMET (el repintado Cumbre es aparte)
    sha256      VARCHAR(64)  NOT NULL,   -- para no re-guardar el mismo frame dos veces
    CONSTRAINT uq_radar_frame UNIQUE (radar_code, captured_at)
);

CREATE INDEX idx_radar_frames_code_time ON radar_frames (radar_code, captured_at DESC);
