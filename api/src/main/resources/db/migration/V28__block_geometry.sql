-- Geometría de la piedra en el mapa: PUNTO (marcador, como hasta ahora) o
-- LÍNEA (muro largo dibujado como polilínea). Es independiente de la modalidad
-- (bloque/vía): un muro largo puede ser de bloque (travesía) o de vía (pared).
-- Aditivo: todas las piedras existentes quedan como POINT.
ALTER TABLE school_blocks
    ADD COLUMN geometry VARCHAR(8) NOT NULL DEFAULT 'POINT';

-- Polilínea (base del muro) como JSON [[lat,lon],...]. Null/vacío si es POINT.
ALTER TABLE school_blocks
    ADD COLUMN path TEXT;

-- Sentido de numeración de las vías a lo largo del muro: LTR (izq→der) / RTL.
ALTER TABLE school_blocks
    ADD COLUMN wall_direction VARCHAR(4) NOT NULL DEFAULT 'LTR';

-- Las propuestas de piedra nueva arrastran la geometría elegida (nullable: las
-- que no son de piedra la dejan a NULL).
ALTER TABLE pending_contributions ADD COLUMN geometry VARCHAR(8);
ALTER TABLE pending_contributions ADD COLUMN path TEXT;
ALTER TABLE pending_contributions ADD COLUMN wall_direction VARCHAR(4);
