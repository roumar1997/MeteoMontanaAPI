-- Descripción opcional del post del feed (la escribe el autor al publicar).
-- Nullable y aditiva: los posts existentes y los automáticos (NEW_BLOCK /
-- NEW_LINE) quedan sin caption.
ALTER TABLE feed_posts ADD COLUMN caption VARCHAR(500);
