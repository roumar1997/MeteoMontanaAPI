-- Notas con foto opcional: la app sube la imagen a Firebase Storage y
-- guarda aquí la URL (mismo patrón que pending_contributions.photo_url).
ALTER TABLE notes ADD COLUMN photo_url TEXT;
