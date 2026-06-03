package com.meteomontana.api.application.users;

/**
 * Vista pública de un usuario: lo que cualquiera puede ver.
 * No incluye email, fcmToken, lastLat/lon, ni prefs de notificación.
 */
public record PublicProfileDto(
        String uid,
        String username,
        String displayName,
        String photoUrl,
        String bio,
        String topGrade
) {}
