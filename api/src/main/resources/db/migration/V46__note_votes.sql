-- Voto de utilidad por usuario en las notas comunitarias (me gusta / no me
-- gusta). Un voto por usuario y nota; los contadores agregados viven en
-- notes.upvotes_count / downvotes_count (ya existían desde V2, sin usar).
CREATE TABLE note_votes (
    id         VARCHAR(160) PRIMARY KEY,   -- noteId:uid
    note_id    VARCHAR(80)  NOT NULL,
    uid        VARCHAR(255) NOT NULL,
    vote_value SMALLINT     NOT NULL,      -- 1 = me gusta, -1 = no me gusta
    CONSTRAINT fk_note_votes_note FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE,
    CONSTRAINT uq_note_votes UNIQUE (note_id, uid)
);

CREATE INDEX idx_note_votes_note ON note_votes (note_id);
CREATE INDEX idx_note_votes_uid ON note_votes (uid);
