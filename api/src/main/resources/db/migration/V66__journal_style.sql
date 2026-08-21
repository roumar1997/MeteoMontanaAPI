-- Estilo de ascension al marcar una via/bloque como hecha: A VISTA y AL FLASH.
-- Independientes entre si (se puede marcar una, la otra, las dos o ninguna) --
-- Rodrigo, 2026-08-21: "que puedas pulsar 1, el otro, los dos o ninguno".
-- No exclusivos con status DONE/PROJECT: solo tienen sentido si status=DONE,
-- pero no se valida a nivel de BD para no complicar (los clientes no las
-- muestran en PROJECT).
ALTER TABLE journal_sessions ADD COLUMN a_vista BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE journal_sessions ADD COLUMN al_flash BOOLEAN NOT NULL DEFAULT FALSE;
