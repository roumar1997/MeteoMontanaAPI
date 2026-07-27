package com.meteomontana.api.domain.model;

import java.time.LocalDateTime;

/**
 * Estado de moderación de un usuario (avisos, suspensión, baneo). Vive aparte
 * de {@link User} a propósito: es información de MODERACIÓN, no del perfil, y
 * solo la manejan la consola de admin y el guard de publicación.
 */
public record UserModerationState(
        String uid,
        String username,
        String displayName,
        boolean banned,
        LocalDateTime suspendedUntil,
        int warnings
) {
    /** true si ahora mismo no puede publicar (baneado o suspensión vigente). */
    public boolean isBlockedNow() {
        return banned || (suspendedUntil != null && suspendedUntil.isAfter(LocalDateTime.now()));
    }
}
