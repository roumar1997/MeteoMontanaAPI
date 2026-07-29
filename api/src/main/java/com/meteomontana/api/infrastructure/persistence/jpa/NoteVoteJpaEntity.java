package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Voto (±1) de un usuario a una nota comunitaria. id = noteId:uid. */
@Entity
@Table(name = "note_votes")
@Getter
public class NoteVoteJpaEntity {

    @Id
    private String id;

    @Column(name = "note_id", nullable = false)
    private String noteId;

    @Column(nullable = false)
    private String uid;

    @Column(name = "vote_value", nullable = false)
    @Setter
    private int voteValue;

    protected NoteVoteJpaEntity() {}

    public NoteVoteJpaEntity(String noteId, String uid, int voteValue) {
        this.id = noteId + ":" + uid;
        this.noteId = noteId;
        this.uid = uid;
        this.voteValue = voteValue;
    }

}
