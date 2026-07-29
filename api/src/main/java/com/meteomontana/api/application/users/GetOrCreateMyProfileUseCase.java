package com.meteomontana.api.application.users;

import com.meteomontana.api.application.journal.JournalUseCase;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.UserRepository;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;

/**
 * Devuelve el perfil del usuario autenticado.
 * Si es su primera petición → lo crea (Just-In-Time provisioning).
 *
 * Así evitamos un endpoint separado de "registro": el simple hecho de
 * pedir GET /api/me crea el usuario en BD si aún no existe.
 */
@Service
@RequiredArgsConstructor
public class GetOrCreateMyProfileUseCase {

    private final UserRepository userRepository;
    private final UserDtoMapper mapper;
    private final JournalUseCase journalUseCase;

    public PrivateProfileDto execute(FirebaseUser firebaseUser) {
        User user = userRepository.findByUid(firebaseUser.uid())
                .orElseGet(() -> createNewUser(firebaseUser));
        // Grado calculado del diario (fuente única). Null si no hay entradas →
        // el mapper cae al campo guardado.
        return mapper.toPrivate(user, journalUseCase.maxGradeFor(user.getUid()));
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
                null,                    // fcmToken — se setea desde PUT /api/me/fcm-token
                null,                    // gender — el usuario lo elige si quiere
                now, now
        );
        return userRepository.save(newUser);
    }
}
