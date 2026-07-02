package com.meteomontana.api.application.users;

import com.meteomontana.api.domain.exception.UserNotFoundException;
import com.meteomontana.api.domain.exception.UsernameAlreadyTakenException;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
public class UpdateMyProfileUseCase {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9_]{3,20}$");

    private final UserRepository userRepository;
    private final UserDtoMapper mapper;

    public UpdateMyProfileUseCase(UserRepository userRepository, UserDtoMapper mapper) {
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    @Transactional
    public PrivateProfileDto execute(String uid, UpdateProfileRequest req) {
        User current = userRepository.findByUid(uid)
                .orElseThrow(() -> new UserNotFoundException(uid));

        String newUsername    = req.username() != null    ? validateUsername(req.username(), uid) : current.getUsername();
        String newDisplayName = req.displayName() != null ? truncate(req.displayName(), 120) : current.getDisplayName();
        String newBio         = req.bio() != null         ? truncate(req.bio(), 150) : current.getBio();
        String newTopGrade    = req.topGrade() != null    ? req.topGrade() : current.getTopGrade();
        boolean newIsPublic   = req.isPublic() != null    ? req.isPublic() : current.isPublic();
        String newPhotoPath   = req.photoUrl() != null    ? req.photoUrl() : current.getPhotoPath();
        String newGender      = req.gender() != null      ? validateGender(req.gender()) : current.getGender();
        String newGearJson    = req.gearJson() != null    ? truncate(req.gearJson(), 512) : current.getGearJson();

        User updated = new User(
                current.getUid(),
                current.getEmail(),
                newUsername,
                newDisplayName,
                newPhotoPath,
                newBio,
                newIsPublic,
                newTopGrade,
                current.isAdmin(),
                current.isPremium(),
                current.getFcmToken(),
                newGender,
                newGearJson,
                current.getCreatedAt(),
                LocalDateTime.now()
        );

        return mapper.toPrivate(userRepository.save(updated));
    }

    private String validateUsername(String username, String currentUid) {
        String normalized = username.toLowerCase().trim();
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "Username must be 3-20 chars, lowercase letters/digits/underscore");
        }
        if (userRepository.usernameTakenByOtherUser(normalized, currentUid)) {
            throw new UsernameAlreadyTakenException(normalized);
        }
        return normalized;
    }

    private String validateGender(String gender) {
        return switch (gender.toUpperCase()) {
            case "WOMAN", "MAN", "OTHER", "UNSPECIFIED" -> gender.toUpperCase();
            default -> throw new IllegalArgumentException("Invalid gender value: " + gender);
        };
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
