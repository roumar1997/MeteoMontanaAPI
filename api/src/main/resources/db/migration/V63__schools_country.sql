-- Pais de cada escuela (ISO 3166-1 alfa-2).
--
-- Todo lo que habia hasta ahora es de Espana: la app nacio con las 191 escuelas
-- espanolas y ni el modelo ni las apps tenian donde poner otro pais. Por eso el
-- DEFAULT es 'ES' y el backfill no necesita distinguir nada.
--
-- El pais decide, ademas del filtro del catalogo, si aplican los servicios de
-- AEMET (radar y boletin de montana), que son solo de Espana.
ALTER TABLE schools
    ADD COLUMN IF NOT EXISTS country VARCHAR(2) NOT NULL DEFAULT 'ES';

ALTER TABLE school_submissions
    ADD COLUMN IF NOT EXISTS proposed_country VARCHAR(2) NOT NULL DEFAULT 'ES';

CREATE INDEX IF NOT EXISTS idx_schools_country ON schools (country);
