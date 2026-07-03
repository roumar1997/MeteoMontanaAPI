package com.meteomontana.api.application.users;

import com.meteomontana.api.domain.exception.UserNotFoundException;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.UserRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataUserDeviceRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.UserDeviceJpaEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UpdateFcmTokenUseCase {

    private final UserRepository userRepository;
    private final SpringDataUserDeviceRepository deviceRepository;

    public UpdateFcmTokenUseCase(UserRepository userRepository,
                                 SpringDataUserDeviceRepository deviceRepository) {
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
    }

    public record FcmTokenRequest(String token) {}

    @Transactional
    public void execute(String uid, String token) {
        User current = userRepository.findByUid(uid)
                .orElseThrow(() -> new UserNotFoundException(uid));

        User updated = new User(
                current.getUid(), current.getEmail(), current.getUsername(),
                current.getDisplayName(), current.getPhotoPath(), current.getBio(),
                current.isPublic(), current.getTopGrade(), current.isAdmin(),
                current.isPremium(), token, current.getGender(),
                current.getCreatedAt(), LocalDateTime.now()
        );
        userRepository.save(updated);

        // Registro por dispositivo: cada app hace PUT con SU token al arrancar,
        // así el usuario acumula un token por aparato (Android + iPhone). Si el
        // token ya existía con otro uid (móvil que cambió de cuenta), se reasigna.
        if (token != null && !token.isBlank()) {
            UserDeviceJpaEntity device = deviceRepository.findById(token)
                    .orElseGet(() -> new UserDeviceJpaEntity(token, uid));
            device.setUid(uid);
            deviceRepository.save(device);
        }
    }
}
