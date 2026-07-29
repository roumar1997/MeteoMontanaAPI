package com.meteomontana.api.application.admin;

import com.meteomontana.api.domain.exception.ForbiddenException;
import com.meteomontana.api.domain.port.UserRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

/**
 * Verifica que el usuario autenticado tiene flag is_admin = true en BD.
 * Cualquier endpoint admin invoca ensureAdmin(uid) al principio.
 *
 * Alternativa: añadir ROLE_ADMIN al FirebaseTokenFilter consultando BD en
 * cada request. Lo dejamos así de simple por ahora.
 */
@Service
@RequiredArgsConstructor
public class AdminGuard {

    private final UserRepository userRepository;

    public void ensureAdmin(String uid) {
        if (!isAdmin(uid)) {
            throw new ForbiddenException("Admin role required");
        }
    }

    /** Comprobación no lanzante (para ramificar comportamiento, p.ej. auto-aprobar). */
    public boolean isAdmin(String uid) {
        return userRepository.findByUid(uid)
                .map(u -> u.isAdmin())
                .orElse(false);
    }
}
