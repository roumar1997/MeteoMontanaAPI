package com.meteomontana.api.application.admin;

import com.meteomontana.api.domain.exception.ForbiddenException;
import com.meteomontana.api.domain.port.UserRepository;
import org.springframework.stereotype.Service;

/**
 * Verifica que el usuario autenticado tiene flag is_admin = true en BD.
 * Cualquier endpoint admin invoca ensureAdmin(uid) al principio.
 *
 * Alternativa: añadir ROLE_ADMIN al FirebaseTokenFilter consultando BD en
 * cada request. Lo dejamos así de simple por ahora.
 */
@Service
public class AdminGuard {

    private final UserRepository userRepository;

    public AdminGuard(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void ensureAdmin(String uid) {
        boolean isAdmin = userRepository.findByUid(uid)
                .map(u -> u.isAdmin())
                .orElse(false);
        if (!isAdmin) {
            throw new ForbiddenException("Admin role required");
        }
    }
}
