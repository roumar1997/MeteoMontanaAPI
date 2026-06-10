package com.meteomontana.api.application.users;

import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.infrastructure.storage.StorageService;
import org.springframework.stereotype.Component;

/**
 * Convierte User → DTOs. Resuelve photoPath → URL firmada usando StorageService.
 */
@Component
public class UserDtoMapper {

    private static final int PHOTO_URL_TTL_MINUTES = 60;

    private final StorageService storageService;

    public UserDtoMapper(StorageService storageService) {
        this.storageService = storageService;
    }

    public PublicProfileDto toPublic(User u) {
        return new PublicProfileDto(
                u.getUid(),
                u.getUsername(),
                u.getDisplayName(),
                resolvePhotoUrl(u.getPhotoPath()),
                u.getBio(),
                u.getTopGrade(),
                false
        );
    }

    /** Vista de un perfil privado para alguien que NO es seguidor: solo datos básicos, sin bio/topGrade. */
    public PublicProfileDto toPublicLocked(User u) {
        return new PublicProfileDto(
                u.getUid(),
                u.getUsername(),
                u.getDisplayName(),
                resolvePhotoUrl(u.getPhotoPath()),
                null,
                null,
                true
        );
    }

    public PrivateProfileDto toPrivate(User u) {
        return new PrivateProfileDto(
                u.getUid(),
                u.getEmail(),
                u.getUsername(),
                u.getDisplayName(),
                resolvePhotoUrl(u.getPhotoPath()),
                u.getBio(),
                u.getTopGrade(),
                u.isPublic(),
                u.isAdmin(),
                u.isPremium()
        );
    }

    private String resolvePhotoUrl(String photoPath) {
        if (photoPath == null || photoPath.isBlank()) return null;
        // Si la app ya guardó una URL completa (Firebase Storage download URL),
        // la devolvemos tal cual — no es un path interno que tengamos que firmar.
        if (photoPath.startsWith("http://") || photoPath.startsWith("https://")) {
            return photoPath;
        }
        try {
            return storageService.signedReadUrl(photoPath, PHOTO_URL_TTL_MINUTES).toString();
        } catch (Exception e) {
            return null;
        }
    }
}
