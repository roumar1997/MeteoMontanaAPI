-- Modalidad de escalada de cada piedra: BOULDER (bloque) o ROUTE (vía).
-- Es una propiedad de la PIEDRA (school_blocks tipo BLOCK); todas sus vías
-- (líneas) heredan la modalidad. Las piedras existentes quedan como BOULDER,
-- porque la app nació centrada en bloque.
ALTER TABLE school_blocks
    ADD COLUMN discipline VARCHAR(16) NOT NULL DEFAULT 'BOULDER';

-- Las propuestas de piedra nueva arrastran la modalidad elegida por el autor.
-- Nullable: las propuestas que no son de piedra (parking/sector/corregir) la
-- dejan a NULL.
ALTER TABLE pending_contributions
    ADD COLUMN discipline VARCHAR(16);

-- Snapshot de la modalidad en cada entrada del diario, para separar el conteo
-- del perfil (BLOQUES vs VÍAS) sin depender del catálogo, que se recicla.
-- Nullable: las entradas antiguas se interpretan como BOULDER al contar.
ALTER TABLE journal_sessions
    ADD COLUMN discipline VARCHAR(16);
