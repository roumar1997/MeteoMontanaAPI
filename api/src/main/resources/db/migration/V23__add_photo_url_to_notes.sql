-- Fotos en notas comunitarias: la app sube la imagen a Firebase Storage
-- (reusa StorageUploadHelper) y aquí solo guardamos la URL pública.
ALTER TABLE notes ADD COLUMN photo_url TEXT;
