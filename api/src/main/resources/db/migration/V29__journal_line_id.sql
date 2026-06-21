-- Enganche estable del diario a la vía: id de la BlockLine (no el número de
-- piedra, que se recicla). Permite mostrar grado/foto/posición EN VIVO y que el
-- deep-link lleve a la vía correcta tras reordenar/cambiar foto. Nullable: las
-- entradas antiguas (y las marcadas offline antes de sincronizar) no lo tienen y
-- caen al match por nombre.
ALTER TABLE journal_sessions ADD COLUMN line_id VARCHAR(64);
