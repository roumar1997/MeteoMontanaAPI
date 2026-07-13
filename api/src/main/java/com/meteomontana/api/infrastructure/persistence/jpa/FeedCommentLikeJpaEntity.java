package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/** Me gusta de un usuario a un COMENTARIO del feed. Un like por usuario. Ver V57. */
@Entity
@Table(name = "feed_comment_likes")
@IdClass(FeedCommentLikeJpaEntity.Key.class)
public class FeedCommentLikeJpaEntity {

    public static class Key implements Serializable {
        private String commentId;
        private String uid;
        public Key() {}
        public Key(String commentId, String uid) { this.commentId = commentId; this.uid = uid; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key k)) return false;
            return Objects.equals(commentId, k.commentId) && Objects.equals(uid, k.uid);
        }
        @Override public int hashCode() { return Objects.hash(commentId, uid); }
    }

    @Id
    @Column(name = "comment_id")
    private String commentId;

    @Id
    private String uid;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected FeedCommentLikeJpaEntity() {}

    public FeedCommentLikeJpaEntity(String commentId, String uid) {
        this.commentId = commentId; this.uid = uid;
    }

    public String getCommentId() { return commentId; }
    public String getUid() { return uid; }
}
