-- Orientación propuesta por el AUTOR al crear una piedra (opcional):
-- JSON {"block":"NE","faces":{"0":"N","2":"S"}}. Al aprobar, se materializa
-- como su primer voto en block_orientation_votes. Aditivo: apps viejas no
-- mandan el campo y nada cambia.
ALTER TABLE pending_contributions ADD COLUMN orientations_json TEXT;
