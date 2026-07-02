-- "Proyecto" (escalada): un bloque/vía que estás probando pero aún no te ha
-- salido. Es el mismo diario (journal_sessions), solo con estado distinto:
-- DONE (hecho, comportamiento de siempre) | PROJECT (proyecto). Al conseguirlo,
-- la entrada PROJECT se borra y se crea una DONE — no se acumulan filas.
ALTER TABLE journal_sessions ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'DONE';
