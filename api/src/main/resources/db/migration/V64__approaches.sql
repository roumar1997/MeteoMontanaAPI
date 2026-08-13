-- Aproximaciones (caminos grabados del parking al sector) y sus chinchetas.
-- Ver APPROACH_DESIGN.md en el repo Android. Fase 1 del plan: solo lectura,
-- sembrado a mano por el admin (GPX importado) — el alta por usuario (grabar,
-- añadir chincheta) llega en una fase posterior, sujeta a revisión legal de
-- los términos (APPROACH_DESIGN.md §2.6/§10).
--
-- No es un school_block: mezclarlo contaminaría todas las consultas de
-- piedras y muros. Tabla propia (APPROACH_DESIGN.md §11).
--
-- IDs como VARCHAR(80), igual que schools/school_blocks (no UUID nativo de
-- Postgres: todo el resto del esquema usa String generado en la app).
--
-- Lección de chk_start_type (bug SEMI, 2.19.0): kind/status/source van
-- VARCHAR SIN CHECK — anadir un valor nuevo al enum no debe exigir migracion.
-- El unico CHECK es el de contenido de la chincheta, invariante de negocio real.
CREATE TABLE approaches (
    id              VARCHAR(80) PRIMARY KEY,
    school_id       VARCHAR(80) NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    from_block_id   VARCHAR(80) REFERENCES school_blocks(id) ON DELETE SET NULL,
    to_block_id     VARCHAR(80) REFERENCES school_blocks(id) ON DELETE SET NULL,
    name            VARCHAR(120),
    path_json       TEXT NOT NULL,
    distance_m      INTEGER,
    ascent_m        INTEGER,
    duration_min    INTEGER,
    source          VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    author_uid      VARCHAR(128) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_approaches_school ON approaches(school_id);

CREATE TABLE approach_pins (
    id           VARCHAR(80) PRIMARY KEY,
    approach_id  VARCHAR(80) NOT NULL REFERENCES approaches(id) ON DELETE CASCADE,
    lat          DOUBLE PRECISION NOT NULL,
    lon          DOUBLE PRECISION NOT NULL,
    position_idx INTEGER NOT NULL,
    kind         VARCHAR(20) NOT NULL,
    message      TEXT,
    photo_path   VARCHAR(255),
    author_uid   VARCHAR(128) NOT NULL,
    status       VARCHAR(20) NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT chk_pin_has_content
        CHECK (message IS NOT NULL OR photo_path IS NOT NULL)
);
CREATE INDEX idx_pins_approach ON approach_pins(approach_id, position_idx);
