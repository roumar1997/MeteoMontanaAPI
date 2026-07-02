-- Fix validación Hibernate (ddl-auto: validate): las entidades usan
-- double/Double (mapea a DOUBLE PRECISION), pero V41 creó esas columnas como
-- NUMERIC → Hibernate falla al arrancar por tipo no coincidente (igual que
-- V19 arregló para columnas int/INTEGER).

ALTER TABLE grip_max_records
    ALTER COLUMN max_kg TYPE DOUBLE PRECISION;

ALTER TABLE grip_measure_sessions
    ALTER COLUMN peak_kg TYPE DOUBLE PRECISION,
    ALTER COLUMN avg_kg  TYPE DOUBLE PRECISION;

ALTER TABLE grip_workout_sets
    ALTER COLUMN target_min_pct TYPE DOUBLE PRECISION,
    ALTER COLUMN target_max_pct TYPE DOUBLE PRECISION;
