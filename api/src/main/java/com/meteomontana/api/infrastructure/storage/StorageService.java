package com.meteomontana.api.infrastructure.storage;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
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
     * Genera una URL firmada de lectura que expira pasados minutesValid minutos.
     * El cliente puede descargar la foto sin tener credenciales de Firebase.
     */
    public URL signedReadUrl(String path, int minutesValid) {
        String bucketName = StorageClient.getInstance().bucket().getName();
        Storage storage = StorageOptions.getDefaultInstance().getService();

        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, path)).build();

        return storage.signUrl(
                blobInfo,
                minutesValid,
                TimeUnit.MINUTES,
                Storage.SignUrlOption.withV4Signature()
        );
    }
}
