-- Para POSITION_CORRECTION: id del bloque (school_block) cuya posición se quiere corregir.
-- NULL si la corrección es sobre la escuela entera (lat/lon de schools).
ALTER TABLE pending_contributions ADD COLUMN target_block_id VARCHAR(80);
