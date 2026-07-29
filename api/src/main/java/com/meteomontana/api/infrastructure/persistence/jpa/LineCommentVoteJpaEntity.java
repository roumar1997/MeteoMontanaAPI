package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Voto (±1) de un usuario a un comentario de piedra/vía. id = commentId:uid. */
@Entity
@Table(name = "line_comment_votes")
@Getter
public class LineCommentVoteJpaEntity {

    @Id
    private String id;

    @Column(name = "comment_id", nullable = false)
    private String commentId;

    @Column(nullable = false)
    private String uid;

    @Column(name = "vote_value", nullable = false)
    @Setter
    private int voteValue;

    protected LineCommentVoteJpaEntity() {}

    public LineCommentVoteJpaEntity(String commentId, String uid, int voteValue) {
        this.id = commentId + ":" + uid;
        this.commentId = commentId;
        this.uid = uid;
        this.voteValue = voteValue;
    }

}
