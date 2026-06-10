-- El check de V11 solo permitía los 4 tipos originales; ASSIGN_SECTOR (asignar
-- un sector existente a una piedra existente) lo violaba y el INSERT fallaba.
ALTER TABLE pending_contributions
    DROP CONSTRAINT chk_contribution_type;

ALTER TABLE pending_contributions
    ADD CONSTRAINT chk_contribution_type
    CHECK (type IN ('PARKING', 'BOULDER', 'SECTOR', 'POSITION_CORRECTION', 'ASSIGN_SECTOR'));
