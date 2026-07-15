package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.infrastructure.security.FirebaseUser;
import com.meteomontana.api.infrastructure.storage.ImageValidation;
import com.meteomontana.api.infrastructure.storage.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Fotos "Tipo A" (piedra/perfil/nota/quedada) en Cloudflare R2. Antes las
 * subían las apps DIRECTAS a Firebase y guardaban su URL pública; ahora:
 *
 *  - Subida: POST /api/photo/upload (multipart) → sube a R2 vía StorageService
 *    y devuelve una URL PERMANENTE {@code {base}/api/photo/{key}}.
 *  - Lectura: GET /api/photo/{key} → 302 a una URL firmada CORTA de R2. El móvil
 *    baja los bytes directo de R2 (egress gratis); el backend solo manda el
 *    redirect. Público (estas fotos ya lo eran en Firebase). Sin caducidad de
 *    la URL guardada porque cada lectura re-firma.
 *
 * Whitelist de prefijos: solo Tipo A. Las de feed/escuela (Tipo B) NO se sirven
 * por aquí (las firma su propio flujo).
 */
@RestController
@RequestMapping("/api/photo")
public class PhotoController {

    /** TTL corto de la URL firmada del redirect (el móvil la usa al instante). */
    private static final int SIGNED_TTL_MINUTES = 15;
    private static final long MAX_BYTES = 5L * 1024 * 1024;

    /** Prefijos servibles por redirect y categorías subibles. */
    private static final Set<String> ALLOWED_PREFIXES = Set.of(
            "piedra-photos-pending/", "note-photos/", "meetup-photos/", "profile-photos/");

    private final StorageService storage;

    public PhotoController(StorageService storage) {
        this.storage = storage;
    }

    // ─────────────────────────────────────────────────────────── lectura

    @GetMapping("/**")
    public ResponseEntity<Void> read(HttpServletRequest req) {
        String key = keyFromPath(req);
        if (key.isBlank() || !hasAllowedPrefix(key)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        URI signed = URI.create(storage.signedReadUrl(key, SIGNED_TTL_MINUTES).toString());
        return ResponseEntity.status(HttpStatus.FOUND).location(signed).build();
    }

    // ─────────────────────────────────────────────────────────── subida

    /**
     * @param category "boulder" (piedra), "note", "meetup" o "profile".
     * @param schoolId opcional (para la clave de piedra/nota, como antes).
     */
    @PostMapping("/upload")
    public Map<String, String> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") String category,
            @RequestParam(value = "schoolId", required = false) String schoolId,
            @RequestParam(value = "meetupId", required = false) String meetupId,
            @AuthenticationPrincipal FirebaseUser user,
            HttpServletRequest request) throws IOException {

        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File too large (max 5MB)");
        }
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files are allowed");
        }
        ImageValidation.ensureRealImage(file); // magic bytes, no solo el content-type

        String uid = user.uid();
        String ext = ext(ct);
        String u = UUID.randomUUID().toString();   // único (evita colisiones en subidas simultáneas)
        String key = switch (category == null ? "" : category.toLowerCase()) {
            // Mismos prefijos que usaban las apps en Firebase (la whitelist de
            // lectura los reconoce); el nombre lleva uid + uuid.
            case "boulder" -> "piedra-photos-pending/" + uid + "_" + safe(schoolId) + "_" + u + ".jpg";
            case "note"    -> "note-photos/" + uid + "_" + safe(schoolId) + "_" + u + ".jpg";
            case "meetup"  -> "meetup-photos/" + safe(meetupId) + "_" + uid + "_" + u + ".jpg";
            case "profile" -> "profile-photos/" + uid + "." + ext;   // 1 por usuario (se sobrescribe)
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "categoría no válida");
        };

        storage.upload(key, file);

        // URL permanente con el host de ESTE entorno (staging o prod).
        return Map.of("url", baseUrl(request) + "/api/photo/" + key);
    }

    // ─────────────────────────────────────────────────────────── helpers

    /** Ruta (key) tras "/api/photo/". Ya viene URL-decoded por el contenedor. */
    private static String keyFromPath(HttpServletRequest req) {
        String full = req.getRequestURI();               // p.ej. /api/photo/note-photos/xxx.jpg
        String ctx = req.getContextPath();
        String p = full.substring(ctx.length());
        String prefix = "/api/photo/";
        return p.length() > prefix.length() ? p.substring(prefix.length()) : "";
    }

    private static boolean hasAllowedPrefix(String key) {
        for (String pre : ALLOWED_PREFIXES) if (key.startsWith(pre)) return true;
        return false;
    }

    private static String ext(String contentType) {
        return switch (contentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }

    private static String safe(String s) {
        return (s == null || s.isBlank()) ? "x" : s.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private static String baseUrl(HttpServletRequest req) {
        // Respeta el host/proto reales (Railway va tras proxy → X-Forwarded-*,
        // que Spring aplica si server.forward-headers-strategy=framework).
        String scheme = req.getScheme();
        String host = req.getHeader("X-Forwarded-Host");
        if (host == null) host = req.getHeader("Host");
        if (host == null) host = req.getServerName();
        String proto = req.getHeader("X-Forwarded-Proto");
        if (proto != null) scheme = proto;
        return scheme + "://" + host;
    }
}
