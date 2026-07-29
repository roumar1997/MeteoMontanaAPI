package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
public class UserJpaEntity {

    @Id
    private String uid;

    @Column(nullable = false)
    private String email;

    private String username;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "photo_path")
    private String photoPath;

    private String bio;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @Column(name = "top_grade")
    private String topGrade;

    @Column(name = "is_admin", nullable = false)
    private boolean isAdmin;

    @Column(name = "is_premium", nullable = false)
    private boolean isPremium;

    @Column(name = "fcm_token")
    private String fcmToken;

    @Column(name = "gender")
    private String gender;

    @Column(name = "gear_json", length = 512)
    private String gearJson;

    // ── Moderación ──────────────────────────────────────────────────────
    @Column(name = "banned", nullable = false)
    @Setter
    private boolean banned;

    @Column(name = "suspended_until")
    @Setter
    private LocalDateTime suspendedUntil;

    @Column(name = "warnings", nullable = false)
    @Setter
    private int warnings;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected UserJpaEntity() {}

    public UserJpaEntity(String uid, String email, String username, String displayName,
                         String photoPath, String bio, boolean isPublic, String topGrade,
                         boolean isAdmin, boolean isPremium, String fcmToken, String gender,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(uid, email, username, displayName, photoPath, bio, isPublic, topGrade,
                isAdmin, isPremium, fcmToken, gender, null, createdAt, updatedAt);
    }

    public UserJpaEntity(String uid, String email, String username, String displayName,
                         String photoPath, String bio, boolean isPublic, String topGrade,
                         boolean isAdmin, boolean isPremium, String fcmToken, String gender,
                         String gearJson, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.uid = uid;
        this.email = email;
        this.username = username;
        this.displayName = displayName;
        this.photoPath = photoPath;
        this.bio = bio;
        this.isPublic = isPublic;
        this.topGrade = topGrade;
        this.isAdmin = isAdmin;
        this.isPremium = isPremium;
        this.fcmToken = fcmToken;
        this.gender = gender;
        this.gearJson = gearJson;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

}
