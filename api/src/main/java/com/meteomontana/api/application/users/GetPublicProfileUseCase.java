package com.meteomontana.api.application.users;

import com.meteomontana.api.domain.exception.UserNotFoundException;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class GetPublicProfileUseCase {

    private final UserRepository userRepository;
    private final UserDtoMapper mapper;

    public GetPublicProfileUseCase(UserRepository userRepository, UserDtoMapper mapper) {
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    /** Busca por uid o username. Devuelve solo si el perfil es público. */
    public PublicProfileDto execute(String identifier) {
        User user = userRepository.findByUid(identifier)
                .or(() -> userRepository.findByUsername(identifier))
                .orElseThrow(() -> new UserNotFoundException(identifier));

        if (!user.isPublic()) {
            // No leakeamos la existencia del perfil privado.
            throw new UserNotFoundException(identifier);
        }
        return mapper.toPublic(user);
    }
}
