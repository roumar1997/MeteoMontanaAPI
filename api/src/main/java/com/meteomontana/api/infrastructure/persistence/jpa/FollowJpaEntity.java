package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "follows")
public class FollowJpaEntity {

    @EmbeddedId
    private FollowId id;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    protected FollowJpaEntity() {}

    public FollowJpaEntity(String followerUid, String followedUid, LocalDateTime createdAt, String status) {
        this.id = new FollowId(followerUid, followedUid);
        this.createdAt = createdAt;
        this.status = status;
    }

    public FollowId getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Embeddable
    public static class FollowId implements Serializable {
        @Column(name = "follower_uid")
        private String followerUid;
        @Column(name = "followed_uid")
        private String followedUid;

        public FollowId() {}
        public FollowId(String followerUid, String followedUid) {
            this.followerUid = followerUid;
            this.followedUid = followedUid;
        }
        public String getFollowerUid() { return followerUid; }
        public String getFollowedUid() { return followedUid; }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FollowId other)) return false;
            return Objects.equals(followerUid, other.followerUid)
                    && Objects.equals(followedUid, other.followedUid);
        }
        @Override public int hashCode() { return Objects.hash(followerUid, followedUid); }
    }
}
