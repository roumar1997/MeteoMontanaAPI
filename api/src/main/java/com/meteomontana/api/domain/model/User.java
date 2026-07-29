package com.meteomontana.api.domain.model;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
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

}
