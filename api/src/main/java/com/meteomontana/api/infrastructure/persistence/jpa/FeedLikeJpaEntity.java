package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/** Me gusta de un usuario a un post del feed. Un like por usuario. Ver V53. */
@Entity
@Table(name = "feed_likes")
@IdClass(FeedLikeJpaEntity.Key.class)
public class FeedLikeJpaEntity {

    public static class Key implements Serializable {
        private Long postId;
        private String uid;
        public Key() {}
        public Key(Long postId, String uid) { this.postId = postId; this.uid = uid; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key k)) return false;
            return Objects.equals(postId, k.postId) && Objects.equals(uid, k.uid);
        }
        @Override public int hashCode() { return Objects.hash(postId, uid); }
    }

    @Id
    @Column(name = "post_id")
    private Long postId;

    @Id
    private String uid;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected FeedLikeJpaEntity() {}

    public FeedLikeJpaEntity(Long postId, String uid) {
        this.postId = postId; this.uid = uid;
    }

    public Long getPostId() { return postId; }
    public String getUid() { return uid; }
}
