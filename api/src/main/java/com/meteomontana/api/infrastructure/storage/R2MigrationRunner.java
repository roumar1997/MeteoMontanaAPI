package com.meteomontana.api.infrastructure.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Migración de fotos Firebase → R2 disparada por la variable de entorno
 * {@code R2_MIGRATE} al arrancar (para no depender de un token admin, que en
 * staging no existe). Valores:
 *   - "dry"  → solo cuenta cuántas faltan en R2 (no copia).
 *   - "copy" → copia de verdad (idempotente; salta lo ya copiado).
 *   - vacío / cualquier otro → no hace nada.
 * Se ejecuta en un hilo aparte para no bloquear el arranque. Tras verla en los
 * logs, quitar la variable. NUNCA borra de Firebase.
 */
@Component
public class R2MigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(R2MigrationRunner.class);

    private final String mode;
    private final StorageMigrationService migration;

    public R2MigrationRunner(@Value("${R2_MIGRATE:}") String mode,
                             StorageMigrationService migration) {
        this.mode = mode == null ? "" : mode.trim().toLowerCase();
        this.migration = migration;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!mode.equals("dry") && !mode.equals("copy")) return;
        boolean dryRun = mode.equals("dry");
        // En hilo aparte: la copia puede tardar y no debe frenar el arranque.
        Thread t = new Thread(() -> {
            try {
                log.info("R2_MIGRATE={} → arrancando migración Firebase→R2...", mode);
                var r = migration.migrate(dryRun);
                log.info("R2_MIGRATE fin: total={}, copiadas/pendientes={}, ya-estaban={}, fallos={}",
                        r.total(), r.copied(), r.skipped(), r.failed());
            } catch (Exception e) {
                log.error("R2_MIGRATE falló: {}", e.getMessage(), e);
            }
        }, "r2-migration");
        t.setDaemon(true);
        t.start();
    }
}
