package com.meteomontana.api.application.users;

/**
 * Vista pública de un usuario: lo que cualquiera puede ver.
 * No incluye email, fcmToken, lastLat/lon, ni prefs de notificación.
 *
 * Si {@code locked = true}, el perfil es privado y el solicitante no es seguidor
 * aceptado: bio/topGrade pueden venir null y la app debe mostrar pantalla de
 * "Sigue para ver" en vez del contenido.
 */
public record PublicProfileDto(
        String uid,
        String username,
        String displayName,
        String photoUrl,
        String bio,
        String topGrade,
        boolean locked
) {}
