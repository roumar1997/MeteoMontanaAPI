-- Añade SCHOOL_NAME_CORRECTION y SCHOOL_STYLE_CORRECTION al CHECK de
-- pending_contributions.type — sin esto, insertar una de estas dos
-- contribuciones revienta con un error de constraint (mismo patrón que el
-- bug de chk_start_type con SEMI, 2026-08-14).
ALTER TABLE pending_contributions
    DROP CONSTRAINT chk_contribution_type;

ALTER TABLE pending_contributions
    ADD CONSTRAINT chk_contribution_type
    CHECK (type IN ('PARKING', 'BOULDER', 'SECTOR', 'POSITION_CORRECTION', 'ASSIGN_SECTOR',
                     'SCHOOL_NAME_CORRECTION', 'SCHOOL_STYLE_CORRECTION'));
