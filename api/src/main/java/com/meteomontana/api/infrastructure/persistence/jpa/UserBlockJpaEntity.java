package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/** Bloqueo entre usuarios: blocker deja de ver contenido de blocked. Ver V50. */
@Entity
@Table(name = "user_blocks")
@IdClass(UserBlockJpaEntity.Key.class)
public class UserBlockJpaEntity {

    public static class Key implements Serializable {
        private String blockerUid;
        private String blockedUid;
        public Key() {}
        public Key(String blockerUid, String blockedUid) {
            this.blockerUid = blockerUid; this.blockedUid = blockedUid;
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key k)) return false;
            return Objects.equals(blockerUid, k.blockerUid) && Objects.equals(blockedUid, k.blockedUid);
        }
        @Override public int hashCode() { return Objects.hash(blockerUid, blockedUid); }
    }

    @Id
    @Column(name = "blocker_uid")
    private String blockerUid;

    @Id
    @Column(name = "blocked_uid")
    private String blockedUid;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected UserBlockJpaEntity() {}

    public UserBlockJpaEntity(String blockerUid, String blockedUid) {
        this.blockerUid = blockerUid; this.blockedUid = blockedUid;
    }

    public String getBlockerUid() { return blockerUid; }
    public String getBlockedUid() { return blockedUid; }
}
