package com.meteomontana.api.application.users;

/**
 * Vista privada del usuario autenticado (`GET /api/me`).
 * Incluye email y flags que el propio usuario debe ver pero nadie más.
 */
public record PrivateProfileDto(
        String uid,
        String email,
        String username,
        String displayName,
        String photoUrl,
        String bio,
        String topGrade,
        boolean isPublic,
        boolean isAdmin,
        boolean isPremium,
        String gender,   // WOMAN | MAN | OTHER | UNSPECIFIED | null — PRIVADO, nunca en PublicProfileDto
        // Material propio: JSON {"cuerda":true,"grigri":false,"cintas":12,"crashpads":2} | null.
        String gearJson
) {}
