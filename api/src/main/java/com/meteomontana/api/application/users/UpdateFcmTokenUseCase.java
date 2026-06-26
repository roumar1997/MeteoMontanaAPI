package com.meteomontana.api.application.users;

import com.meteomontana.api.domain.exception.UserNotFoundException;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UpdateFcmTokenUseCase {

    private final UserRepository userRepository;

    public UpdateFcmTokenUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
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
    }
}
