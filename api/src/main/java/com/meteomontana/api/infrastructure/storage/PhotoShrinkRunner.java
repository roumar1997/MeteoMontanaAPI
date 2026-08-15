package com.meteomontana.api.infrastructure.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Reducción de las fotos YA subidas, disparada por la variable de entorno
 * {@code PHOTO_SHRINK} al arrancar (mismo patrón que {@link R2MigrationRunner}:
 * evita depender de un token admin). Valores:
 *   - "dry" → solo calcula el ahorro, NO escribe nada.
 *   - "run" → reduce de verdad (guardando antes el original en originals/).
 *   - vacío / cualquier otro → no hace nada.
 *
 * En hilo aparte para no frenar el arranque. Tras verlo en los logs, QUITAR la
 * variable (si no, se re-ejecuta en cada despliegue; es idempotente, pero
 * gasta tiempo y operaciones de R2 para nada).
 */
@Component
public class PhotoShrinkRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PhotoShrinkRunner.class);

    private final String mode;
    private final PhotoShrinkService shrink;

    public PhotoShrinkRunner(@Value("${PHOTO_SHRINK:}") String mode,
                             PhotoShrinkService shrink) {
        this.mode = mode == null ? "" : mode.trim().toLowerCase();
        this.shrink = shrink;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!mode.equals("dry") && !mode.equals("run")) return;
        boolean dryRun = mode.equals("dry");

        Thread t = new Thread(() -> {
            try {
                log.info("PHOTO_SHRINK={} → reduciendo fotos ya subidas...", mode);
                var r = shrink.shrinkAll(dryRun);
                log.info("PHOTO_SHRINK fin: escaneadas={}, reducidas={}, saltadas={}, fallos={}",
                        r.scanned(), r.shrunk(), r.skipped(), r.failed());
                log.info("PHOTO_SHRINK ahorro: {} MB → {} MB ({} MB menos)",
                        r.bytesBefore() / (1024 * 1024),
                        r.bytesAfter() / (1024 * 1024),
                        (r.bytesBefore() - r.bytesAfter()) / (1024 * 1024));
                if (r.firstError() != null) log.warn("PHOTO_SHRINK primer error: {}", r.firstError());
            } catch (Exception e) {
                log.error("PHOTO_SHRINK falló: {}", e.getMessage(), e);
            }
        }, "photo-shrink");
        t.setDaemon(true);
        t.start();
    }
}
