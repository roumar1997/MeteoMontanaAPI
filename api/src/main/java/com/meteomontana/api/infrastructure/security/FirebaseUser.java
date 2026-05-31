package com.meteomontana.api.infrastructure.security;

/**
 * Representa al usuario autenticado tal como lo conoce el back.
 * Spring Security lo mete en el SecurityContext tras validar el token.
 *
 * uid   → identificador único de Firebase (inmutable, nunca cambia)
 * email → email de la cuenta Google del usuario
 * name  → nombre visible (puede ser null si el token no lo incluye)
 */
public record FirebaseUser(String uid, String email, String name) {
}
