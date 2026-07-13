package com.meteomontana.api.infrastructure.storage;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.firebase.cloud.StorageClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Encapsula Firebase Storage. El resto de la app no toca com.google.* —
 * habla con este service.
 */
@Service
public class StorageService {

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
     * Genera una URL firmada de lectura que expira pasados minutesValid minutos.
     * El cliente puede descargar la foto sin tener credenciales de Firebase.
     */
    public URL signedReadUrl(String path, int minutesValid) {
        String bucketName = StorageClient.getInstance().bucket().getName();

        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, path)).build();

        return storage().signUrl(
                blobInfo,
                minutesValid,
                TimeUnit.MINUTES,
                Storage.SignUrlOption.withV4Signature()
        );
    }
}
