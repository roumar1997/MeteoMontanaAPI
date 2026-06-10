-- Corrección de una vía concreta de un bloque existente.
-- Si target_line_id != null, la contribución BOULDER reemplaza esa línea
-- (nombre, grado, tipo de inicio, line_path) al aprobarse.
ALTER TABLE pending_contributions ADD COLUMN target_line_id VARCHAR(80);
