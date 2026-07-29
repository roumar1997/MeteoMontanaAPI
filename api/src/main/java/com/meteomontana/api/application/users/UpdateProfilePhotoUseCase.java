package com.meteomontana.api.application.users;

import com.meteomontana.api.domain.exception.UserNotFoundException;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.UserRepository;
import com.meteomontana.api.infrastructure.storage.ImageValidation;
import com.meteomontana.api.infrastructure.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateProfilePhotoUseCase {

    private final UserRepository userRepository;
    private final StorageService storageService;

    // NO @Transactional: la subida a Firebase Storage (segundos de red) no debe
    // retener una conexión del pool. findByUid y save cogen/sueltan su conexión
    // en milisegundos; la subida ocurre entre medias sin ninguna abierta.
    public PrivateProfileDto execute(String uid, MultipartFile file, UserDtoMapper mapper) throws IOException {
        User current = userRepository.findByUid(uid)
                .orElseThrow(() -> new UserNotFoundException(uid));

        if (file.isEmpty()) throw new IllegalArgumentException("Empty file");
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/"))
            throw new IllegalArgumentException("Only image files allowed");
        if (file.getSize() > 5 * 1024 * 1024)
            throw new IllegalArgumentException("File too large (max 5MB)");
        // Comprueba que los bytes son de una imagen real, no solo el Content-Type.
        ImageValidation.ensureRealImage(file);

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
                current.getFcmToken(), current.getGender(),
                current.getCreatedAt(), LocalDateTime.now()
        );
        return mapper.toPrivate(userRepository.save(updated));
    }
}
