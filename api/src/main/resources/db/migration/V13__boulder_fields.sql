-- BOULDER contributions: photo URL from Firebase Storage, bloques JSON, topo lines JSON.
ALTER TABLE pending_contributions
    ADD COLUMN photo_url    TEXT,
    ADD COLUMN bloques_json TEXT,
    ADD COLUMN topo_lines_json TEXT;
