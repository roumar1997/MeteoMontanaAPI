package com.meteomontana.api.infrastructure.storage;

import java.net.URL;
import java.util.List;

/**
 * Operaciones crudas de almacenamiento de objetos. Dos implementaciones:
 * {@link FirebaseStorageBackend} (histórico) y {@link R2StorageBackend}
 * (Cloudflare R2, egress gratis). {@link StorageService} elige una según la
 * variable {@code STORAGE_BACKEND} y le pone delante la caché de URLs firmadas.
 */
public interface StorageBackend {

    /** Sube [bytes] a [path] con el content-type dado. */
    void upload(String path, byte[] bytes, String contentType);

    /** Borra el objeto en [path] (idempotente: si no existe, no falla). */
    void delete(String path);

    /** URL firmada de lectura que caduca a los [minutesValid] minutos. */
    URL signedReadUrl(String path, int minutesValid);

    /** Descarga los bytes de [path], o null si no existe. Solo para migración. */
    byte[] readBytes(String path);

    /** Lista todas las rutas de objetos del bucket. Solo para migración. */
    List<String> listAll();
}
