-- Relación piedra→sector: una piedra (BLOCK) puede pertenecer a un sector (ZONE)
-- de la misma escuela. Nullable (los datos antiguos quedan sin sector).
ALTER TABLE school_blocks
    ADD COLUMN sector_block_id VARCHAR;

ALTER TABLE school_blocks
    ADD CONSTRAINT fk_school_blocks_sector
    FOREIGN KEY (sector_block_id) REFERENCES school_blocks(id) ON DELETE SET NULL;

CREATE INDEX idx_school_blocks_sector ON school_blocks(sector_block_id);

-- Las propuestas necesitan poder transportar el sector destino:
--   · BOULDER       → sector al que se asigna la piedra nueva (opcional).
--   · ASSIGN_SECTOR → sector que se asigna a una piedra existente.
ALTER TABLE pending_contributions
    ADD COLUMN sector_block_id VARCHAR;
