package com.meteomontana.api.infrastructure.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Reescritura de las URLs guardadas al CDN directo, disparada por la variable
 * {@code PHOTO_DIRECT_URLS} al arrancar (mismo patrón que
 * {@link R2MigrationRunner} y {@link PhotoShrinkRunner}). Valores:
 *   - "dry" → solo cuenta cuántas se cambiarían, NO escribe.
 *   - "run" → reescribe de verdad.
 *   - vacío / cualquier otro → no hace nada.
 *
 * Usa {@code R2_PUBLIC_URL} como destino. Tras verlo en los logs, QUITAR la
 * variable.
 */
@Component
public class PhotoDirectUrlRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PhotoDirectUrlRunner.class);

    private final String mode;
    private final String cdnBase;
    private final PhotoDirectUrlService service;

    public PhotoDirectUrlRunner(@Value("${PHOTO_DIRECT_URLS:}") String mode,
                                @Value("${R2_PUBLIC_URL:}") String cdnBase,
                                PhotoDirectUrlService service) {
        this.mode = mode == null ? "" : mode.trim().toLowerCase();
        this.cdnBase = cdnBase == null ? "" : cdnBase.trim();
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!mode.equals("dry") && !mode.equals("run")) return;
        boolean dryRun = mode.equals("dry");

        Thread t = new Thread(() -> {
            try {
                log.info("PHOTO_DIRECT_URLS={} → apuntando las fotos directas a {}", mode, cdnBase);
                var r = service.rewriteAll(cdnBase, dryRun);
                log.info("PHOTO_DIRECT_URLS fin: escaneadas={}, reescritas={}, saltadas={}",
                        r.scanned(), r.rewritten(), r.skipped());
            } catch (Exception e) {
                log.error("PHOTO_DIRECT_URLS falló: {}", e.getMessage(), e);
            }
        }, "photo-direct-urls");
        t.setDaemon(true);
        t.start();
    }
}
