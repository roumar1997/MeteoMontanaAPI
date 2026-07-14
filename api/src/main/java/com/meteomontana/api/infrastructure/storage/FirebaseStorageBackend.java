package com.meteomontana.api.infrastructure.storage;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.firebase.cloud.StorageClient;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Backend de Firebase Storage (histórico). Se usa cuando
 * {@code STORAGE_BACKEND != r2}. Encapsula todo el uso de com.google.* de
 * almacenamiento; el resto de la app no lo toca (habla con StorageService).
 */
@Component
public class FirebaseStorageBackend implements StorageBackend {

    @Override
    public void upload(String path, byte[] bytes, String contentType) {
        StorageClient.getInstance().bucket().create(path, bytes, contentType);
    }

    @Override
    public void delete(String path) {
        Blob blob = StorageClient.getInstance().bucket().get(path);
        if (blob != null) blob.delete();
    }

    /**
     * Firma con el cliente del SDK de Firebase Admin (lleva la clave privada de
     * la service account). El StorageOptions.getDefaultInstance() dependía de
     * las Application Default Credentials, inexistentes en Railway → firma rota
     * (destapado por la foto del feed el 2026-07-13).
     */
    @Override
    public URL signedReadUrl(String path, int minutesValid) {
        String bucketName = StorageClient.getInstance().bucket().getName();
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, path)).build();
        return storage().signUrl(blobInfo, minutesValid, TimeUnit.MINUTES,
                Storage.SignUrlOption.withV4Signature());
    }

    @Override
    public byte[] readBytes(String path) {
        Blob blob = StorageClient.getInstance().bucket().get(path);
        return blob == null ? null : blob.getContent();
    }

    @Override
    public List<String> listAll() {
        List<String> paths = new ArrayList<>();
        for (Blob blob : StorageClient.getInstance().bucket().list().iterateAll()) {
            if (!blob.isDirectory()) paths.add(blob.getName());
        }
        return paths;
    }

    private Storage storage() {
        return StorageClient.getInstance().bucket().getStorage();
    }
}
