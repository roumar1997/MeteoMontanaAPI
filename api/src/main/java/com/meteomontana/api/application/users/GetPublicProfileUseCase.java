package com.meteomontana.api.application.users;

import com.meteomontana.api.application.journal.JournalUseCase;
import com.meteomontana.api.domain.exception.UserNotFoundException;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.FollowRepository;
import com.meteomontana.api.domain.port.UserRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetPublicProfileUseCase {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final UserDtoMapper mapper;
    private final JournalUseCase journalUseCase;

    /**
     * Busca por uid o username. Devuelve el perfil completo si es público,
     * el propio, o si el solicitante ya es seguidor aceptado. Si no, devuelve
     * un perfil "locked" con solo los datos básicos (nombre, foto) para que el
     * cliente muestre la pantalla de "Sigue para ver".
     */
    public PublicProfileDto execute(String identifier, String currentUid) {
        User user = userRepository.findByUid(identifier)
                .or(() -> userRepository.findByUsername(identifier))
                .orElseThrow(() -> new UserNotFoundException(identifier));

        // Grado calculado del diario (mismo que muestra la app), no el campo viejo.
        String grade = journalUseCase.maxGradeFor(user.getUid());

        if (user.isPublic()) return mapper.toPublic(user, grade);

        boolean isSelf = currentUid != null && currentUid.equals(user.getUid());
        boolean isAcceptedFollower = currentUid != null
                && followRepository.isFollowing(currentUid, user.getUid());
        if (isSelf || isAcceptedFollower) return mapper.toPublic(user, grade);

        return mapper.toPublicLocked(user);
    }
}
