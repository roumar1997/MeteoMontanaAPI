-- Snapshot enriquecido del feed: modalidad de la vía (BOULDER | ROUTE) y tipo
-- de roca de la escuela en el momento de publicar. Nullable a propósito: los
-- posts anteriores a esta migración quedan a NULL y la app pinta genérico.
-- Aditivo puro (compatible con APKs ya instalados y con ddl-auto=validate).

ALTER TABLE feed_posts ADD COLUMN discipline VARCHAR(20);
ALTER TABLE feed_posts ADD COLUMN rock_type  VARCHAR(40);
