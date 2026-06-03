package com.meteomontana.api.application.users;

import com.meteomontana.api.domain.exception.UserNotFoundException;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.UserRepository;
import com.meteomontana.api.infrastructure.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
public class UpdateProfilePhotoUseCase {

    private final UserRepository userRepository;
    private final StorageService storageService;

    public UpdateProfilePhotoUseCase(UserRepository userRepository, StorageService storageService) {
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    @Transactional
    public PrivateProfileDto execute(String uid, MultipartFile file, UserDtoMapper mapper) throws IOException {
        User current = userRepository.findByUid(uid)
                .orElseThrow(() -> new UserNotFoundException(uid));

        if (file.isEmpty()) throw new IllegalArgumentException("Empty file");
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/"))
            throw new IllegalArgumentException("Only image files allowed");
        if (file.getSize() > 5 * 1024 * 1024)
            throw new IllegalArgumentException("File too large (max 5MB)");

        String extension = switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png"  -> "png";
            case "image/webp" -> "webp";
            default           -> "bin";
        };
        String path = "profile-photos/" + uid + "." + extension;

        // Borra foto anterior si era distinto path
        if (current.getPhotoPath() != null && !current.getPhotoPath().equals(path)) {
            try { storageService.delete(current.getPhotoPath()); } catch (Exception ignored) {}
        }

        storageService.upload(path, file);

        User updated = new User(
                current.getUid(), current.getEmail(), current.getUsername(),
                current.getDisplayName(), path, current.getBio(),
                current.isPublic(), current.getTopGrade(),
                current.isAdmin(), current.isPremium(),
                current.getFcmToken(),
                current.getCreatedAt(), LocalDateTime.now()
        );
        return mapper.toPrivate(userRepository.save(updated));
    }
}
