-- Foto de celebración opcional del post del feed (la hace el autor con la
-- cámara al publicar el ascenso). Guarda la RUTA en Firebase Storage
-- (feed-photos/{postId}/{uuid}.{ext}); la URL firmada se genera al leer.
-- Nullable y aditiva: los posts existentes quedan sin foto.
ALTER TABLE feed_posts ADD COLUMN photo_path VARCHAR(500);
