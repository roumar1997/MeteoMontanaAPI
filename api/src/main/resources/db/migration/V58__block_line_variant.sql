-- Variante opcional de una vía (p.ej. "directa", "extensión", "desde el pie").
-- Permite que dos vías homónimas ("La ola" y "La ola (extensión)") tengan
-- identidad propia visible. Aditivo puro: nullable, sin defaults.
ALTER TABLE block_lines ADD COLUMN variant VARCHAR(60);
