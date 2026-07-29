package com.meteomontana.api.infrastructure.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

/**
 * Copia todas las fotos de Firebase Storage → Cloudflare R2. Se dispara a mano
 * desde un endpoint admin (POST /api/admin/storage/migrate). Idempotente: una
 * foto que ya está en R2 se salta, así se puede re-ejecutar sin duplicar ni
 * romper nada. NUNCA borra de Firebase (el rollback es volver a
 * STORAGE_BACKEND=firebase).
 */
@Service
@RequiredArgsConstructor
public class StorageMigrationService {

    private static final Logger log = LoggerFactory.getLogger(StorageMigrationService.class);

    public record Result(int total, int copied, int skipped, int failed, String firstError) {}

    private final FirebaseStorageBackend firebase;
    private final R2StorageBackend r2;

    /**
     * @param dryRun si true, solo cuenta objetos en Firebase y cuántos faltan en
     *               R2, sin copiar nada (para verificar antes de lanzar).
     */
    public Result migrate(boolean dryRun) {
        if (!r2.isConfigured()) {
            throw new IllegalStateException("R2 no configurado (faltan variables R2_*).");
        }
        var paths = firebase.listAll();
        int total = paths.size(), copied = 0, skipped = 0, failed = 0;
        String firstError = null;
        log.info("Migración Firebase→R2: {} objetos en Firebase (dryRun={}).", total, dryRun);

        for (String path : paths) {
            try {
                // Ya en R2 → saltar (idempotente; permite re-ejecutar).
                if (r2.readBytes(path) != null) {
                    skipped++;
                    continue;
                }
                if (dryRun) {
                    copied++; // "se copiaría"
                    continue;
                }
                byte[] bytes = firebase.readBytes(path);
                if (bytes == null) { failed++; continue; }
                r2.upload(path, bytes, guessContentType(path));
                copied++;
                if (copied % 50 == 0) log.info("  ...{} copiadas", copied);
            } catch (Exception e) {
                log.warn("Fallo copiando {}: {}", path, e.getMessage());
                if (firstError == null) firstError = e.getClass().getSimpleName() + ": " + e.getMessage();
                failed++;
            }
        }
        log.info("Migración Firebase→R2 fin: total={}, copiadas/pendientes={}, ya-estaban={}, fallos={}",
                total, copied, skipped, failed);
        return new Result(total, copied, skipped, failed, firstError);
    }

    private static String guessContentType(String path) {
        String p = path.toLowerCase();
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".webp")) return "image/webp";
        if (p.endsWith(".gif")) return "image/gif";
        return "image/jpeg";
    }
}
