-- Mismo problema que V35 (quedadas): Hibernate 6 mapea String de Java como
-- 'character varying', y Postgres no compara uuid = character varying sin
-- cast explícito -> "Schema-validation: wrong column type ... found [uuid],
-- expecting [varchar(255)]" -> la app no arranca. Convertimos todas las
-- columnas UUID de agarres a varchar(36), igual que el resto del esquema.
-- Los IDs se generan en Java (UUID.randomUUID().toString()), el DEFAULT
-- gen_random_uuid() no se usa nunca — hay que quitarlo antes del ALTER.

-- FK primero (grip_workout_sets.workout_id -> grip_workouts.id)
ALTER TABLE grip_workout_sets DROP CONSTRAINT IF EXISTS grip_workout_sets_workout_id_fkey;

ALTER TABLE grip_max_records ALTER COLUMN id DROP DEFAULT;
ALTER TABLE grip_max_records ALTER COLUMN id TYPE varchar(36) USING id::text;

ALTER TABLE grip_measure_sessions ALTER COLUMN id DROP DEFAULT;
ALTER TABLE grip_measure_sessions ALTER COLUMN id TYPE varchar(36) USING id::text;

ALTER TABLE grip_workouts ALTER COLUMN id DROP DEFAULT;
ALTER TABLE grip_workouts ALTER COLUMN id TYPE varchar(36) USING id::text;

ALTER TABLE grip_workout_sets ALTER COLUMN id DROP DEFAULT;
ALTER TABLE grip_workout_sets ALTER COLUMN id TYPE varchar(36) USING id::text;
ALTER TABLE grip_workout_sets ALTER COLUMN workout_id TYPE varchar(36) USING workout_id::text;

ALTER TABLE grip_workout_sets ADD CONSTRAINT grip_workout_sets_workout_id_fkey
    FOREIGN KEY (workout_id) REFERENCES grip_workouts(id) ON DELETE CASCADE;
