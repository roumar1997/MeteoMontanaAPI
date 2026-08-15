package com.meteomontana.api.infrastructure.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Re-comprime las fotos que YA estaban subidas (las nuevas entran reducidas
 * desde PhotoController). Se dispara a mano desde un endpoint admin.
 *
 * Idempotente y seguro de repetir: una foto que ya pesa poco se salta, y si
 * reducir no gana tamaño se deja la original intacta. Sobrescribe la MISMA
 * clave, así que las URLs guardadas en la BD siguen siendo válidas y no hay
 * que tocar ni la BD ni las apps.
 *
 * REVERSIBLE: antes de sobrescribir, el original se copia a {@code originals/}
 * dentro del mismo bucket. Ocupa el doble (unos cientos de MB, con 10 GB
 * gratis), pero permite volver atrás y re-derivar otra calidad en el futuro.
 * Aun así, el modo dryRun deja ver el ahorro antes de escribir nada.
 */
@Service
@RequiredArgsConstructor
public class PhotoShrinkService {

    private static final Logger log = LoggerFactory.getLogger(PhotoShrinkService.class);

    /** Por debajo de esto no merece la pena tocar nada. */
    private static final int SKIP_UNDER_BYTES = 400 * 1024;

    /** Copia de seguridad del original, para poder deshacer. */
    private static final String BACKUP_PREFIX = "originals/";

    /** Prefijos de fotos "Tipo A" (las que sirve /api/photo). */
    private static final List<String> PREFIXES = List.of(
            "piedra-photos-pending/", "note-photos/", "meetup-photos/",
            "profile-photos/", "approach-pins/");

    public record Result(int scanned, int shrunk, int skipped, int failed,
                         long bytesBefore, long bytesAfter, String firstError) {}

    private final R2StorageBackend r2;

    /** @param dryRun si true, calcula el ahorro sin escribir nada. */
    public Result shrinkAll(boolean dryRun) {
        if (!r2.isConfigured()) {
            throw new IllegalStateException("R2 no configurado (faltan variables R2_*).");
        }
        int scanned = 0, shrunk = 0, skipped = 0, failed = 0;
        long before = 0, after = 0;
        String firstError = null;

        for (String key : r2.listAll()) {
            if (key.startsWith(BACKUP_PREFIX)) continue;   // no reducir las copias
            if (PREFIXES.stream().noneMatch(key::startsWith)) continue;
            scanned++;
            try {
                byte[] original = r2.readBytes(key);
                if (original == null || original.length < SKIP_UNDER_BYTES) {
                    skipped++;
                    continue;
                }
                int maxSide = key.startsWith("profile-photos/")
                        ? ImageResizer.MAX_AVATAR_SIDE : ImageResizer.MAX_PHOTO_SIDE;
                byte[] small = ImageResizer.shrink(original, maxSide);
                if (small.length >= original.length) {   // no compensa
                    skipped++;
                    continue;
                }
                before += original.length;
                after += small.length;
                if (!dryRun) {
                    // Copia de seguridad ANTES de sobrescribir. Si ya existe es
                    // que esta clave ya se procesó: no la pisamos (perderíamos
                    // el original al re-ejecutar sobre una foto ya reducida).
                    String backupKey = BACKUP_PREFIX + key;
                    if (r2.readBytes(backupKey) == null) {
                        r2.upload(backupKey, original, "image/jpeg");
                    }
                    r2.upload(key, small, "image/jpeg");
                }
                shrunk++;
                if (shrunk % 25 == 0) log.info("  ...{} fotos reducidas", shrunk);
            } catch (Exception e) {
                log.warn("Fallo reduciendo {}: {}", key, e.toString());
                if (firstError == null) firstError = e.getClass().getSimpleName() + ": " + e.getMessage();
                failed++;
            }
        }
        log.info("Reducción de fotos fin (dryRun={}): escaneadas={}, reducidas={}, saltadas={}, "
                        + "fallos={}, antes={} MB, después={} MB",
                dryRun, scanned, shrunk, skipped, failed,
                before / (1024 * 1024), after / (1024 * 1024));
        return new Result(scanned, shrunk, skipped, failed, before, after, firstError);
    }
}
