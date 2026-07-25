package com.meteomontana.api.application.feed;

import com.meteomontana.api.domain.exception.ForbiddenException;
import com.meteomontana.api.domain.exception.NotFoundException;
import com.meteomontana.api.infrastructure.persistence.jpa.FeedPostJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataFeedPostRepository;
import com.meteomontana.api.infrastructure.storage.ImageValidation;
import com.meteomontana.api.infrastructure.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * FOTO DE CELEBRACIÓN del post: subir/reemplazar en Storage, validación de
 * imagen real (magic bytes) y borrado best-effort.
 */
@Service
public class FeedPhotoService {

    private static final Logger log = LoggerFactory.getLogger(FeedPhotoService.class);

    private static final long MAX_PHOTO_BYTES = 5 * 1024 * 1024;

    private final SpringDataFeedPostRepository posts;
    private final StorageService storage;

    public FeedPhotoService(SpringDataFeedPostRepository posts, StorageService storage) {
        this.posts = posts;
        this.storage = storage;
    }

    /**
     * Sube (o reemplaza) la foto de celebración de un post PROPIO. Mismo patrón
     * que UploadSchoolPhotoUseCase: NO es @Transactional a propósito — la
     * subida a Storage tarda segundos y retendría una conexión del pool; cada
     * acceso a BD coge y suelta la suya. Si el post ya tenía foto, la anterior
     * se borra del Storage (best effort) DESPUÉS de guardar la nueva ruta.
     *
     * @return URL firmada de la foto subida.
     */
    public String uploadPhoto(String uid, long postId, MultipartFile file) throws IOException {
        FeedPostJpaEntity post = posts.findById(postId)
                .orElseThrow(() -> new NotFoundException("post no encontrado"));
        if (!post.getUserUid().equals(uid)) {
            throw new ForbiddenException(
                    "solo puedes añadir foto a tus posts");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Empty file");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }
        if (file.getSize() > MAX_PHOTO_BYTES) {
            throw new IllegalArgumentException("File too large (max 5MB)");
        }
        // Comprueba que los bytes son de una imagen real, no solo el Content-Type.
        ImageValidation.ensureRealImage(file);

        String previousPath = post.getPhotoPath();
        String storagePath = "feed-photos/" + postId + "/" + UUID.randomUUID()
                + "." + guessExtension(contentType);
        storage.upload(storagePath, file);

        try {
            post.setPhotoPath(storagePath);
            posts.save(post);
        } catch (RuntimeException ex) {
            // Si Postgres falla, intentamos limpiar el archivo huérfano.
            deletePhotoQuietly(storagePath);
            throw ex;
        }
        // Reemplazo: la foto anterior ya no está referenciada → fuera del Storage.
        if (previousPath != null && !previousPath.equals(storagePath)) {
            deletePhotoQuietly(previousPath);
        }
        return storage.signedReadUrl(storagePath, FeedViewMapper.PHOTO_URL_TTL_MINUTES).toString();
    }

    /** Borra una foto del Storage, best effort: nunca tumba la operación. */
    public void deletePhotoQuietly(String photoPath) {
        if (photoPath == null || photoPath.isBlank()) return;
        try {
            storage.delete(photoPath);
        } catch (Exception e) {
            log.warn("No se pudo borrar la foto del feed {}: {}", photoPath, e.getMessage());
        }
    }

    private static String guessExtension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png"  -> "png";
            case "image/webp" -> "webp";
            case "image/gif"  -> "gif";
            default           -> "bin";
        };
    }
}
