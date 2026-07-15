package com.meteomontana.api.infrastructure.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Reescribe las URLs de fotos "Tipo A" guardadas en la BD: de la URL de Firebase
 * (que las apps guardaban al subir directas) a la URL permanente del backend
 * {@code {base}/api/photo/{key}} (que redirige a R2). Idempotente y NO
 * destructivo: solo cambia filas cuya URL es de Firebase; las ya migradas o
 * relativas (Tipo B) no se tocan. Reversible mientras Firebase siga vivo.
 */
@Service
public class PhotoUrlRewriteService {

    private static final Logger log = LoggerFactory.getLogger(PhotoUrlRewriteService.class);

    /** Solo se reescriben claves de estos prefijos (Tipo A). */
    private static final List<String> ALLOWED = List.of(
            "piedra-photos-pending/", "note-photos/", "meetup-photos/", "profile-photos/");

    /** (tabla, columna) con URLs de Firebase Tipo A. */
    private static final Map<String, String> TARGETS = Map.of(
            "users", "photo_path",
            "notes", "photo_url",
            "meetups", "photo_url",
            "pending_contributions", "photo_url",
            "block_lines", "photo_path",
            "school_blocks", "photo_path");

    public record Result(int scanned, int rewritten, int skipped) {}

    private final JdbcTemplate jdbc;

    public PhotoUrlRewriteService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * @param base   URL base del entorno (p.ej. https://api.climbingteams.com).
     * @param dryRun si true, solo cuenta (no escribe).
     */
    public Result rewriteAll(String base, boolean dryRun) {
        int scanned = 0, rewritten = 0, skipped = 0;
        for (var e : TARGETS.entrySet()) {
            String table = e.getKey(), col = e.getValue();
            List<String> urls = jdbc.queryForList(
                    "SELECT " + col + " FROM " + table +
                    // Sin '/' tras .com: algunas URLs llevan puerto explícito
                    // (":443") → el patrón antiguo con '/' no las cazaba.
                    " WHERE " + col + " LIKE 'https://firebasestorage.googleapis.com%'",
                    String.class);
            for (String url : urls) {
                scanned++;
                String key = keyFromFirebaseUrl(url);
                if (key == null || !allowed(key)) { skipped++; continue; }
                String newUrl = base + "/api/photo/" + key;
                if (!dryRun) {
                    jdbc.update("UPDATE " + table + " SET " + col + " = ? WHERE " + col + " = ?",
                            newUrl, url);
                }
                rewritten++;
            }
            log.info("Rewrite {}.{}: {} URLs de Firebase encontradas", table, col, urls.size());
        }
        log.info("Rewrite URLs fin (dryRun={}): escaneadas={}, reescritas={}, saltadas={}",
                dryRun, scanned, rewritten, skipped);
        return new Result(scanned, rewritten, skipped);
    }

    /** Extrae y decodifica la key de una URL de descarga de Firebase. */
    static String keyFromFirebaseUrl(String url) {
        int o = url.indexOf("/o/");
        if (o < 0) return null;
        String after = url.substring(o + 3);
        int q = after.indexOf('?');
        if (q >= 0) after = after.substring(0, q);
        return URLDecoder.decode(after, StandardCharsets.UTF_8);
    }

    private static boolean allowed(String key) {
        for (String p : ALLOWED) if (key.startsWith(p)) return true;
        return false;
    }
}
