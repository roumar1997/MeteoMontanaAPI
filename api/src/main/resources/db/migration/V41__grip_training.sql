-- Pestaña "Agarres": dinamómetro BLE WH-C06, máximos de fuerza y entrenos
-- personalizados. Ver GRIPS_DESIGN.md en el repo Android.

-- Catálogo fijo de combinaciones agarre = dedos/posición × estilo.
CREATE TABLE grip_types (
    id            SERIAL PRIMARY KEY,
    finger_group  VARCHAR(16) NOT NULL,   -- FIVE | FOUR | THREE | FRONT_TWO | MID_TWO
    style         VARCHAR(16) NOT NULL,   -- CRIMP | HALF_CRIMP | DRAG
    UNIQUE(finger_group, style)
);

INSERT INTO grip_types (finger_group, style) VALUES
    ('FIVE', 'CRIMP'), ('FIVE', 'HALF_CRIMP'), ('FIVE', 'DRAG'),
    ('FOUR', 'CRIMP'), ('FOUR', 'HALF_CRIMP'), ('FOUR', 'DRAG'),
    ('THREE', 'CRIMP'), ('THREE', 'HALF_CRIMP'), ('THREE', 'DRAG'),
    ('FRONT_TWO', 'CRIMP'), ('FRONT_TWO', 'HALF_CRIMP'), ('FRONT_TWO', 'DRAG'),
    ('MID_TWO', 'CRIMP'), ('MID_TWO', 'HALF_CRIMP'), ('MID_TWO', 'DRAG');

-- Tu máximo vigente por agarre + mano (1 fila = el récord actual).
CREATE TABLE grip_max_records (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    uid           VARCHAR NOT NULL,
    grip_type_id  INT NOT NULL REFERENCES grip_types(id),
    hand          VARCHAR(8) NOT NULL,    -- LEFT | RIGHT
    max_kg        NUMERIC(6,2) NOT NULL,
    edge_mm       VARCHAR(16) NULL,
    measured_at   TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(uid, grip_type_id, hand)
);

-- Historial de cada test de "Medir" (para la gráfica de progreso).
CREATE TABLE grip_measure_sessions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    uid           VARCHAR NOT NULL,
    grip_type_id  INT NOT NULL REFERENCES grip_types(id),
    hand          VARCHAR(8) NOT NULL,
    peak_kg       NUMERIC(6,2) NOT NULL,
    avg_kg        NUMERIC(6,2) NOT NULL,
    duration_s    INT NOT NULL,
    edge_mm       VARCHAR(16) NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);

-- Plantillas de entreno personalizadas.
CREATE TABLE grip_workouts (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    uid                  VARCHAR NOT NULL,
    name                 VARCHAR(80) NOT NULL,
    hand_mode            VARCHAR(16) NOT NULL,   -- UNA | POR_SERIE | POR_REP
    count_mode           VARCHAR(16) NOT NULL,   -- TIEMPO | PESO
    rest_between_sets_s  INT NOT NULL DEFAULT 0,
    created_at           TIMESTAMP NOT NULL DEFAULT now(),
    updated_at           TIMESTAMP NOT NULL DEFAULT now()
);

-- Sets de una plantilla, en orden.
CREATE TABLE grip_workout_sets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workout_id      UUID NOT NULL REFERENCES grip_workouts(id) ON DELETE CASCADE,
    sort_order      INT NOT NULL,
    reps            INT NOT NULL,
    work_s          INT NOT NULL,
    rest_s          INT NOT NULL,
    grip_type_id    INT NOT NULL REFERENCES grip_types(id),
    target_min_pct  NUMERIC(5,1) NOT NULL,
    target_max_pct  NUMERIC(5,1) NOT NULL
);

CREATE INDEX idx_grip_max_records_uid       ON grip_max_records(uid);
CREATE INDEX idx_grip_measure_sessions_uid  ON grip_measure_sessions(uid, grip_type_id, hand);
CREATE INDEX idx_grip_workouts_uid          ON grip_workouts(uid);
CREATE INDEX idx_grip_workout_sets_workout  ON grip_workout_sets(workout_id, sort_order);
