package com.meteomontana.api.application.photos;

import com.meteomontana.api.domain.exception.SchoolNotFoundException;
import com.meteomontana.api.domain.model.SchoolPhoto;
import com.meteomontana.api.domain.port.SchoolPhotoRepository;
import com.meteomontana.api.domain.port.SchoolRepository;
import com.meteomontana.api.infrastructure.storage.ImageValidation;
import com.meteomontana.api.infrastructure.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Sube una foto de una escuela.
 * Flujo:
 *   1. Verifica que la escuela existe (si no → 404).
 *   2. Sube el archivo a Firebase Storage (school-photos/{schoolId}/{uuid}.{ext}).
 *   3. Inserta la metadata en Postgres.
 *
 * NO es @Transactional a propósito: la subida a Firebase Storage tarda segundos
 * (red) y, si estuviera dentro de la transacción, mantendría OCUPADA una
 * conexión del pool todo ese tiempo → con varias subidas a la vez se agota el
 * pool y el resto de peticiones esperan/fallan. Aquí cada acceso a BD (findById,
 * save) coge y suelta su conexión en milisegundos; la subida ocurre entre medias
 * sin retener ninguna. Si el save falla, borramos la foto huérfana (best effort).
 */
@Service
public class UploadSchoolPhotoUseCase {

    private final SchoolRepository schoolRepository;
    private final SchoolPhotoRepository photoRepository;
    private final StorageService storageService;

    public UploadSchoolPhotoUseCase(SchoolRepository schoolRepository,
                                    SchoolPhotoRepository photoRepository,
                                    StorageService storageService) {
        this.schoolRepository = schoolRepository;
        this.photoRepository = photoRepository;
        this.storageService = storageService;
    }

    public SchoolPhoto execute(String schoolId, String uploaderUid,
                               MultipartFile file, String caption) throws IOException {
        // 1. Validar escuela
        schoolRepository.findById(schoolId)
                .orElseThrow(() -> new SchoolNotFoundException(schoolId));

        // 2. Validar archivo
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Empty file");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File too large (max 5MB)");
        }
        // Comprueba que los bytes son de una imagen real, no solo el Content-Type.
        ImageValidation.ensureRealImage(file);

        // 3. Subir a Storage
        String photoId = UUID.randomUUID().toString();
        String extension = guessExtension(contentType);
        String storagePath = "school-photos/" + schoolId + "/" + photoId + "." + extension;

        storageService.upload(storagePath, file);

        // 4. Insertar metadata en Postgres
        try {
            SchoolPhoto photo = new SchoolPhoto(
                    photoId, schoolId, storagePath, uploaderUid,
                    caption, null, null, file.getSize(),
                    contentType, LocalDateTime.now()
            );
            return photoRepository.save(photo);
        } catch (RuntimeException ex) {
            // Si Postgres falla, intentamos limpiar el archivo huérfano.
            try { storageService.delete(storagePath); } catch (Exception ignored) {}
            throw ex;
        }
    }

    private String guessExtension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png"  -> "png";
            case "image/webp" -> "webp";
            case "image/gif"  -> "gif";
            default           -> "bin";
        };
    }
}
