package com.meteomontana.api.infrastructure.storage;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.firebase.cloud.StorageClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Encapsula Firebase Storage. El resto de la app no toca com.google.* —
 * habla con este service.
 */
@Service
public class StorageService {

    /**
     * Caché de URLs firmadas. Firmar es una operación RSA (~ms); una página de
     * feed con 20 fotos firma 20 URLs, y cada scroll/refresco re-firma las
     * MISMAS fotos. Cachear la URL firmada por ruta evita re-firmar hasta que
     * está a punto de caducar → recorta mucha CPU cuando el feed se usa mucho.
     * La servimos solo mientras le quede holgura (80% de su validez) para no
     * devolver nunca una URL casi caducada.
     */
    private record CachedUrl(URL url, long serveUntilMillis) {}

    private final Map<String, CachedUrl> urlCache = new ConcurrentHashMap<>();
    private static final int URL_CACHE_MAX = 20_000;

    /** Sube un archivo a la ruta indicada dentro del bucket por defecto. */
    public String upload(String path, MultipartFile file) throws IOException {
        StorageClient.getInstance().bucket().create(
                path,
                file.getBytes(),
                file.getContentType()
        );
        return path;
    }

    /** Borra un archivo del bucket. */
    public void delete(String path) {
        var blob = StorageClient.getInstance().bucket().get(path);
        if (blob != null) blob.delete();
    }

    /**
     * Cliente de Storage para FIRMAR URLs: el del SDK de Firebase Admin, que
     * lleva las credenciales de la service account (clave privada → puede
     * firmar). El {@code StorageOptions.getDefaultInstance()} de antes
     * dependía de las Application Default Credentials del entorno, que en
     * Railway NO existen → "Signing key was not provided and could not be
     * derived" (la firma llevaba rota en staging y prod; lo destapó la foto
     * de celebración del feed el 2026-07-13). Firebase Admin cachea su
     * instancia internamente, no hace falta double-checked locking.
     */
    private Storage storage() {
        return StorageClient.getInstance().bucket().getStorage();
    }

    /**
     * Genera (o reutiliza de caché) una URL firmada de lectura que expira
     * pasados minutesValid minutos. El cliente puede descargar la foto sin
     * tener credenciales de Firebase.
     */
    public URL signedReadUrl(String path, int minutesValid) {
        String key = minutesValid + ":" + path;
        long now = System.currentTimeMillis();

        CachedUrl cached = urlCache.get(key);
        if (cached != null && now < cached.serveUntilMillis()) {
            return cached.url();
        }

        String bucketName = StorageClient.getInstance().bucket().getName();
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, path)).build();
        URL url = storage().signUrl(
                blobInfo,
                minutesValid,
                TimeUnit.MINUTES,
                Storage.SignUrlOption.withV4Signature()
        );

        // Servir de caché solo el 80% de la validez → la URL devuelta siempre
        // tiene ≥20% de vida por delante.
        long serveUntil = now + (long) (minutesValid * 60_000L * 0.8);
        // Cota de memoria: si crece demasiado (muchas rutas distintas con el
        // tiempo), purgamos las caducadas; si aun así sigue grande, se vacía.
        if (urlCache.size() >= URL_CACHE_MAX) {
            urlCache.entrySet().removeIf(e -> now >= e.getValue().serveUntilMillis());
            if (urlCache.size() >= URL_CACHE_MAX) urlCache.clear();
        }
        urlCache.put(key, new CachedUrl(url, serveUntil));
        return url;
    }
}
