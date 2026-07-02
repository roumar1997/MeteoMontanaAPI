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
        String photoUrl,
        String gender,      // WOMAN | MAN | OTHER | UNSPECIFIED — privado, nunca en PublicProfileDto
        // Material propio: JSON {"cuerda":true,"grigri":false,"cintas":12,"crashpads":2}.
        // Privado — se usa para autorrellenar el material al unirte a una quedada.
        String gearJson
) {}
