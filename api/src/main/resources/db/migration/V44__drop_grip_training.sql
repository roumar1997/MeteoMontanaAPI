-- Feature "Agarres" (dinamómetro WH-C06) ELIMINADA por decisión de producto
-- (2026-07-02): se retiran las tablas creadas en V41-V43. Las migraciones
-- V41-V43 NO se borran (ya están aplicadas; Flyway valida el historial).
DROP TABLE IF EXISTS grip_workout_sets;
DROP TABLE IF EXISTS grip_workouts;
DROP TABLE IF EXISTS grip_measure_sessions;
DROP TABLE IF EXISTS grip_max_records;
DROP TABLE IF EXISTS grip_types;
