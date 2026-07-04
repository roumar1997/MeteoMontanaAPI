package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Voto (±1) de un usuario a un comentario de piedra/vía. id = commentId:uid. */
@Entity
@Table(name = "line_comment_votes")
public class LineCommentVoteJpaEntity {

    @Id
    private String id;

    @Column(name = "comment_id", nullable = false)
    private String commentId;

    @Column(nullable = false)
    private String uid;

    @Column(name = "vote_value", nullable = false)
    private int voteValue;

    protected LineCommentVoteJpaEntity() {}

    public LineCommentVoteJpaEntity(String commentId, String uid, int voteValue) {
        this.id = commentId + ":" + uid;
        this.commentId = commentId;
        this.uid = uid;
        this.voteValue = voteValue;
    }

    public String getId() { return id; }
    public String getCommentId() { return commentId; }
    public String getUid() { return uid; }
    public int getVoteValue() { return voteValue; }
    public void setVoteValue(int voteValue) { this.voteValue = voteValue; }
}
