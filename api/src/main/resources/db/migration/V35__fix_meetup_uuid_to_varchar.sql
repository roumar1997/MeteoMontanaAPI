-- Hibernate 6 mapea campos String de Java como 'character varying' en JDBC.
-- Postgres no permite comparar uuid = character varying sin cast explícito.
-- Solución: convertir todas las columnas UUID de las tablas de quedadas a
-- varchar(36), que es como el resto del esquema trata los UUIDs generados.

-- ── meetups ──────────────────────────────────────────────────────────────────
-- Primero hay que eliminar las FKs que apuntan a meetups.id
ALTER TABLE meetup_days    DROP CONSTRAINT IF EXISTS meetup_days_meetup_id_fkey;
ALTER TABLE meetup_members DROP CONSTRAINT IF EXISTS meetup_members_meetup_id_fkey;
ALTER TABLE meetup_reports DROP CONSTRAINT IF EXISTS meetup_reports_meetup_id_fkey;

-- PK de meetups
ALTER TABLE meetups ALTER COLUMN id TYPE varchar(36) USING id::text;

-- ── meetup_days ──────────────────────────────────────────────────────────────
-- La PK de meetup_days es (meetup_id, day): hay que recrearla
ALTER TABLE meetup_days DROP CONSTRAINT IF EXISTS meetup_days_pkey;
ALTER TABLE meetup_days ALTER COLUMN meetup_id TYPE varchar(36) USING meetup_id::text;
ALTER TABLE meetup_days ADD PRIMARY KEY (meetup_id, day);

-- ── meetup_members ───────────────────────────────────────────────────────────
ALTER TABLE meetup_members DROP CONSTRAINT IF EXISTS meetup_members_pkey;
ALTER TABLE meetup_members ALTER COLUMN meetup_id TYPE varchar(36) USING meetup_id::text;
ALTER TABLE meetup_members ADD PRIMARY KEY (meetup_id, uid);

-- ── meetup_reports ───────────────────────────────────────────────────────────
ALTER TABLE meetup_reports ALTER COLUMN id        TYPE varchar(36) USING id::text;
ALTER TABLE meetup_reports ALTER COLUMN meetup_id TYPE varchar(36) USING meetup_id::text;

-- ── meetup_alerts ────────────────────────────────────────────────────────────
ALTER TABLE meetup_alerts ALTER COLUMN id TYPE varchar(36) USING id::text;
-- school_id puede ser UUID (V33) o varchar(36) (V31); normalizamos a varchar(36)
-- para que coincida con schools.id (VARCHAR(80))
ALTER TABLE meetup_alerts ALTER COLUMN school_id TYPE varchar(36) USING school_id::text;

-- Restaurar FKs (ahora todos los tipos coinciden)
ALTER TABLE meetup_days ADD CONSTRAINT meetup_days_meetup_id_fkey
    FOREIGN KEY (meetup_id) REFERENCES meetups(id) ON DELETE CASCADE;

ALTER TABLE meetup_members ADD CONSTRAINT meetup_members_meetup_id_fkey
    FOREIGN KEY (meetup_id) REFERENCES meetups(id) ON DELETE CASCADE;

ALTER TABLE meetup_reports ADD CONSTRAINT meetup_reports_meetup_id_fkey
    FOREIGN KEY (meetup_id) REFERENCES meetups(id) ON DELETE CASCADE;
