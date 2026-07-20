-- La release 2.19.0 añadió el inicio SEMI (semi-sentado/incorporado) al enum
-- BlockLine.StartType de la app, pero la columna block_lines.start_type conserva
-- el CHECK de V10 que solo permitía SIT/STAND/JUMP/TRAV. Resultado: CUALQUIER vía
-- con inicio SEMI violaba chk_start_type al insertar → las piedras nuevas que
-- contenían una vía SEMI NO se podían crear al aprobar (cazado en staging el
-- 2026-07-20 con la checklist de validación; el enum se creyó "sin migración"
-- por ser VARCHAR, olvidando el CHECK).
ALTER TABLE block_lines DROP CONSTRAINT chk_start_type;
ALTER TABLE block_lines ADD CONSTRAINT chk_start_type
    CHECK (start_type IS NULL OR start_type IN ('SIT', 'STAND', 'JUMP', 'TRAV', 'SEMI'));
