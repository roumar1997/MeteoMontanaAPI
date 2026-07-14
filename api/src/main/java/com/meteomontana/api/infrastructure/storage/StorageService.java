package com.meteomontana.api.infrastructure.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fachada de almacenamiento de objetos (fotos). El resto de la app habla SOLO
 * con este service, no con com.google.* ni con el SDK de S3.
 *
 * Elige el backend según la variable {@code STORAGE_BACKEND}:
 *  - {@code r2}       → Cloudflare R2 (egress gratis).
 *  - cualquier otro   → Firebase Storage (histórico, por defecto).
 *
 * Migración segura: se despliega con STORAGE_BACKEND=firebase (sin cambio de
 * comportamiento), se copian las fotos a R2 con el endpoint de migración, y
 * solo entonces se pone STORAGE_BACKEND=r2. Volver atrás = poner firebase.
 */
@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private record CachedUrl(URL url, long serveUntilMillis) {}

    private final Map<String, CachedUrl> urlCache = new ConcurrentHashMap<>();
    private static final int URL_CACHE_MAX = 20_000;

    private final StorageBackend backend;
    private final FirebaseStorageBackend firebase;
    private final R2StorageBackend r2;

    public StorageService(
            @Value("${STORAGE_BACKEND:firebase}") String backendName,
            FirebaseStorageBackend firebase,
            R2StorageBackend r2) {
        this.firebase = firebase;
        this.r2 = r2;
        boolean useR2 = "r2".equalsIgnoreCase(backendName.trim());
        if (useR2 && !r2.isConfigured()) {
            // Fallar rápido y claro en el arranque en vez de romper la 1ª foto.
            throw new IllegalStateException(
                    "STORAGE_BACKEND=r2 pero R2 no está configurado (faltan variables R2_*).");
        }
        this.backend = useR2 ? r2 : firebase;
        log.info("StorageService usando backend: {}", useR2 ? "R2" : "Firebase");
    }

    /** Sube un archivo (multipart) a [path]. Devuelve la ruta guardada. */
    public String upload(String path, MultipartFile file) throws IOException {
        backend.upload(path, file.getBytes(), file.getContentType());
        return path;
    }

    /** Borra un archivo. Invalida su URL cacheada. */
    public void delete(String path) {
        backend.delete(path);
        urlCache.keySet().removeIf(k -> k.endsWith(":" + path));
    }

    /**
     * URL firmada de lectura (caché por el 80% de su validez, para no re-firmar
     * las mismas fotos en cada scroll del feed; ver histórico del 2026-07-13).
     */
    public URL signedReadUrl(String path, int minutesValid) {
        String key = minutesValid + ":" + path;
        long now = System.currentTimeMillis();

        CachedUrl cached = urlCache.get(key);
        if (cached != null && now < cached.serveUntilMillis()) {
            return cached.url();
        }

        URL url = backend.signedReadUrl(path, minutesValid);

        long serveUntil = now + (long) (minutesValid * 60_000L * 0.8);
        if (urlCache.size() >= URL_CACHE_MAX) {
            urlCache.entrySet().removeIf(e -> now >= e.getValue().serveUntilMillis());
            if (urlCache.size() >= URL_CACHE_MAX) urlCache.clear();
        }
        urlCache.put(key, new CachedUrl(url, serveUntil));
        return url;
    }

    // ---------------------------------------------------------------- migración

    /** Backends crudos para el migrador (copia Firebase → R2). */
    FirebaseStorageBackend firebaseBackend() { return firebase; }
    R2StorageBackend r2Backend() { return r2; }
}
