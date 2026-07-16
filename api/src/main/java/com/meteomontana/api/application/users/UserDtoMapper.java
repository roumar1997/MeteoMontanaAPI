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
        return toPublic(u, null);
    }

    /**
     * Igual que {@link #toPublic(User)} pero permite sustituir el grado por uno
     * calculado en vivo del diario. Si {@code topGradeOverride} es null, cae al
     * campo guardado {@code top_grade}. Se usa en el perfil individual para que
     * el grado que se comparte coincida con el que muestra la app.
     */
    public PublicProfileDto toPublic(User u, String topGradeOverride) {
        return new PublicProfileDto(
                u.getUid(),
                u.getUsername(),
                u.getDisplayName(),
                resolvePhotoUrl(u.getPhotoPath()),
                u.getBio(),
                topGradeOverride != null ? topGradeOverride : u.getTopGrade(),
                false,
                u.isPublic()
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
                true,
                false
        );
    }

    public PrivateProfileDto toPrivate(User u) {
        return toPrivate(u, null);
    }

    /**
     * Igual que {@link #toPrivate(User)} pero permite sustituir el grado por uno
     * calculado en vivo del diario (null → cae al campo guardado).
     */
    public PrivateProfileDto toPrivate(User u, String topGradeOverride) {
        return new PrivateProfileDto(
                u.getUid(),
                u.getEmail(),
                u.getUsername(),
                u.getDisplayName(),
                resolvePhotoUrl(u.getPhotoPath()),
                u.getBio(),
                topGradeOverride != null ? topGradeOverride : u.getTopGrade(),
                u.isPublic(),
                u.isAdmin(),
                u.isPremium(),
                u.getGender(),
                u.getGearJson()
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
