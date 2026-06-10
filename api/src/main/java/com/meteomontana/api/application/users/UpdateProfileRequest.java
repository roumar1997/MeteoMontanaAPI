package com.meteomontana.api.application.users;

/**
 * Body de `PUT /api/me`. Todos los campos opcionales — el cliente envía
 * solo los que quiere cambiar. Los null se ignoran.
 */
public record UpdateProfileRequest(
        String username,
        String displayName,
        String bio,
        String topGrade,
        Boolean isPublic,
        String photoUrl
) {}
