-- Caras de una piedra (multi-foto).
-- Una piedra grande no cabe en una sola foto: ahora cada vía (block_line) guarda
-- la FOTO sobre la que está dibujada y el ORDEN de su cara. Una "cara" = grupo de
-- vías que comparten la misma foto. La piedra sigue teniendo su photo_path como
-- portada (= primera cara) por retrocompatibilidad.
--
-- Retro: las vías existentes heredan la foto de su bloque y quedan en la cara 0,
-- así nada cambia para las piedras de una sola foto.

ALTER TABLE block_lines ADD COLUMN photo_path TEXT;
ALTER TABLE block_lines ADD COLUMN face_order INTEGER NOT NULL DEFAULT 0;

UPDATE block_lines bl
   SET photo_path = (SELECT b.photo_path FROM school_blocks b WHERE b.id = bl.block_id)
 WHERE bl.photo_path IS NULL;
