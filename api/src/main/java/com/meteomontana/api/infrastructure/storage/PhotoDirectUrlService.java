package com.meteomontana.api.infrastructure.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Reescribe las URLs de fotos guardadas para que apunten DIRECTAMENTE al CDN
 * ({@code https://photos.climbingteams.com/{key}}) en vez de al redirect del
 * backend ({@code {base}/api/photo/{key}}).
 *
 * Por qué: el redirect obliga al móvil a hablar con DOS servidores distintos
 * (backend y CDN), con su DNS, su TCP y su TLS cada uno. Medido el 2026-08-16
 * sobre una foto real: 2,9-4,7 s con redirect contra 1,8-2,7 s directo — un 40%
 * del tiempo se iba en el salto. Con mala cobertura (el caso de uso: en la
 * roca) la diferencia es mayor todavía.
 *
 * Idempotente y NO destructivo: solo toca filas cuya URL es del redirect; las
 * ya directas, las de Firebase y las rutas relativas se quedan como están.
 * Reversible reescribiendo al revés. {@code /api/photo/**} sigue funcionando,
 * así que nada se rompe mientras haya URLs viejas por ahí.
 */
@Service
@RequiredArgsConstructor
public class PhotoDirectUrlService {

    private static final Logger log = LoggerFactory.getLogger(PhotoDirectUrlService.class);

    /** Marca del redirect dentro de la URL guardada. */
    private static final String MARKER = "/api/photo/";

    /** (tabla, columna) con URLs de fotos "Tipo A". */
    private static final Map<String, String> TARGETS = Map.of(
            "users", "photo_path",
            "notes", "photo_url",
            "meetups", "photo_url",
            "pending_contributions", "photo_url",
            "block_lines", "photo_path",
            "school_blocks", "photo_path");

    public record Result(int scanned, int rewritten, int skipped) {}

    private final JdbcTemplate jdbc;

    /**
     * @param cdnBase base del CDN sin barra final (p.ej. https://photos.climbingteams.com).
     * @param dryRun  si true, solo cuenta (no escribe).
     */
    public Result rewriteAll(String cdnBase, boolean dryRun) {
        if (cdnBase == null || cdnBase.isBlank()) {
            throw new IllegalStateException("Falta R2_PUBLIC_URL: sin CDN no hay a dónde apuntar.");
        }
        String base = cdnBase.trim().replaceAll("/+$", "");

        int scanned = 0, rewritten = 0, skipped = 0;
        for (var e : TARGETS.entrySet()) {
            String table = e.getKey(), col = e.getValue();
            List<String> urls = jdbc.queryForList(
                    "SELECT " + col + " FROM " + table +
                    " WHERE " + col + " LIKE '%" + MARKER + "%'", String.class);
            for (String url : urls) {
                scanned++;
                String direct = toDirect(url, base);
                if (direct == null) { skipped++; continue; }
                if (!dryRun) {
                    jdbc.update("UPDATE " + table + " SET " + col + " = ? WHERE " + col + " = ?",
                            direct, url);
                }
                rewritten++;
            }
            log.info("Directo {}.{}: {} URLs con redirect encontradas", table, col, urls.size());
        }
        log.info("Reescritura a CDN directo fin (dryRun={}): escaneadas={}, reescritas={}, saltadas={}",
                dryRun, scanned, rewritten, skipped);
        return new Result(scanned, rewritten, skipped);
    }

    /**
     * {@code https://api.../api/photo/piedra-photos-pending/x.jpg?v=1} →
     * {@code https://photos.../piedra-photos-pending/x.jpg?v=1}.
     * Conserva el query (la foto de perfil lleva ?v= para romper la caché).
     *
     * @return la URL directa, o null si no es una URL de redirect reconocible.
     */
    static String toDirect(String url, String cdnBase) {
        if (url == null) return null;
        int i = url.indexOf(MARKER);
        if (i < 0) return null;
        String keyAndQuery = url.substring(i + MARKER.length());
        if (keyAndQuery.isBlank()) return null;
        return cdnBase + "/" + keyAndQuery;
    }
}
