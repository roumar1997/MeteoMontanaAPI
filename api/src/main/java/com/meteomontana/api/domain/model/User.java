package com.meteomontana.api.domain.model;

import java.time.LocalDateTime;

public class User {
    private final String uid;
    private final String email;
    private final String username;
    private final String displayName;
    private final String photoPath;
    private final String bio;
    private final boolean isPublic;
    private final String topGrade;
    private final boolean isAdmin;
    private final boolean isPremium;
    private final String fcmToken;
    private final String gender;           // WOMAN | MAN | OTHER | UNSPECIFIED | null — PRIVADO
    // Material propio (perfil): JSON simple, mismo formato que meetup_members.gear_json.
    // {"cuerda":true,"grigri":false,"cintas":12,"crashpads":2}. Se usa para
    // autorrellenar el material al unirte a una quedada (editable ahí igualmente).
    private final String gearJson;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public User(String uid, String email, String username, String displayName,
                String photoPath, String bio, boolean isPublic, String topGrade,
                boolean isAdmin, boolean isPremium, String fcmToken, String gender,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(uid, email, username, displayName, photoPath, bio, isPublic, topGrade,
                isAdmin, isPremium, fcmToken, gender, null, createdAt, updatedAt);
    }

    public User(String uid, String email, String username, String displayName,
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
    public String getGender()          { return gender; }
    public String getGearJson()        { return gearJson; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
    public LocalDateTime getUpdatedAt(){ return updatedAt; }
}
