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
@Table(name = "favorites")
public class FavoriteJpaEntity {

    @EmbeddedId
    private FavoriteId id;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected FavoriteJpaEntity() {}

    public FavoriteJpaEntity(String uid, String schoolId, LocalDateTime createdAt) {
        this.id = new FavoriteId(uid, schoolId);
        this.createdAt = createdAt;
    }

    public FavoriteId getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Embeddable
    public static class FavoriteId implements Serializable {
        private String uid;
        @Column(name = "school_id")
        private String schoolId;

        public FavoriteId() {}
        public FavoriteId(String uid, String schoolId) {
            this.uid = uid;
            this.schoolId = schoolId;
        }
        public String getUid() { return uid; }
        public String getSchoolId() { return schoolId; }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FavoriteId other)) return false;
            return Objects.equals(uid, other.uid) && Objects.equals(schoolId, other.schoolId);
        }
        @Override public int hashCode() { return Objects.hash(uid, schoolId); }
    }
}
