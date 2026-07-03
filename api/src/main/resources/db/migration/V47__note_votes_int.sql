-- HOTFIX: V46 creó vote_value como SMALLINT pero la entidad JPA lo mapea como
-- int (integer). Hibernate en modo validate rechaza el desajuste y la app no
-- arrancaba (502 en prod y staging el 2026-07-03). Alinear el tipo.
ALTER TABLE note_votes ALTER COLUMN vote_value TYPE INT;
