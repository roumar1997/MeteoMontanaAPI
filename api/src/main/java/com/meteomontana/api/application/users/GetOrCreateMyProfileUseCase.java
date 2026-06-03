package com.meteomontana.api.application.users;

import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.UserRepository;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Devuelve el perfil del usuario autenticado.
 * Si es su primera petición → lo crea (Just-In-Time provisioning).
 *
 * Así evitamos un endpoint separado de "registro": el simple hecho de
 * pedir GET /api/me crea el usuario en BD si aún no existe.
 */
@Service
public class GetOrCreateMyProfileUseCase {

    private final UserRepository userRepository;
    private final UserDtoMapper mapper;

    public GetOrCreateMyProfileUseCase(UserRepository userRepository, UserDtoMapper mapper) {
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    public PrivateProfileDto execute(FirebaseUser firebaseUser) {
        User user = userRepository.findByUid(firebaseUser.uid())
                .orElseGet(() -> createNewUser(firebaseUser));
        return mapper.toPrivate(user);
    }

    private User createNewUser(FirebaseUser firebaseUser) {
        LocalDateTime now = LocalDateTime.now();
        User newUser = new User(
                firebaseUser.uid(),
                firebaseUser.email(),
                null,                    // username — el usuario lo elige luego
                firebaseUser.name(),     // display name viene de Google
                null,                    // photo path — se sube aparte
                null,                    // bio
                true,                    // perfil público por defecto
                null,                    // top grade
                false,                   // isAdmin
                false,                   // isPremium
                now, now
        );
        return userRepository.save(newUser);
    }
}
