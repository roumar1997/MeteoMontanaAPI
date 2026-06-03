package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
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

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected UserJpaEntity() {}

    public UserJpaEntity(String uid, String email, String username, String displayName,
                         String photoPath, String bio, boolean isPublic, String topGrade,
                         boolean isAdmin, boolean isPremium, String fcmToken,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {
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
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getUid()             { return uid; }
    public String getEmail()           { return email; }
    public String getUsername()        { return username; }
    public String getDisplayName()     { return displayName; }
    public String getPhotoPath()       { return photoPath; }
    public String getBio()             { return bio; }
    public boolean isPublic()          { return isPublic; }
    public String getTopGrade()        { return topGrade; }
    public boolean isAdmin()           { return isAdmin; }
    public boolean isPremium()         { return isPremium; }
    public String getFcmToken()        { return fcmToken; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
    public LocalDateTime getUpdatedAt(){ return updatedAt; }
}
