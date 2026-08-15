package com.meteomontana.api.infrastructure.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Backend de Cloudflare R2 (S3-compatible, egress gratis). Activo cuando
 * {@code STORAGE_BACKEND=r2}. Credenciales por variables de entorno
 * (R2_ACCOUNT_ID, R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY, R2_ENDPOINT,
 * R2_BUCKET). El bean se crea SIEMPRE pero solo abre conexión si está
 * configurado (así el arranque no falla en local/prod-Firebase sin las vars).
 */
@Component
public class R2StorageBackend implements StorageBackend {

    private static final Logger log = LoggerFactory.getLogger(R2StorageBackend.class);
    /** R2 ignora la región pero el SDK exige una; "auto" es la convención de R2. */
    private static final Region R2_REGION = Region.of("auto");

    private final String bucket;
    private final String publicUrl;     // "" si no hay Public Development URL / dominio propio
    private final S3Client client;      // null si no está configurado
    private final S3Presigner presigner;

    public R2StorageBackend(
            @Value("${R2_ENDPOINT:}") String endpoint,
            @Value("${R2_ACCESS_KEY_ID:}") String accessKeyId,
            @Value("${R2_SECRET_ACCESS_KEY:}") String secretAccessKey,
            @Value("${R2_BUCKET:}") String bucket,
            @Value("${R2_PUBLIC_URL:}") String publicUrl) {
        // trim(): un espacio/salto de línea accidental en la variable de Railway
        // rompía R2 con "bucket name is not valid" (400).
        endpoint = endpoint == null ? "" : endpoint.trim();
        accessKeyId = accessKeyId == null ? "" : accessKeyId.trim();
        secretAccessKey = secretAccessKey == null ? "" : secretAccessKey.trim();
        bucket = bucket == null ? "" : bucket.trim();
        publicUrl = publicUrl == null ? "" : publicUrl.trim();
        this.bucket = bucket;
        // Sin barra final, para poder concatenar "/" + key sin duplicarla.
        this.publicUrl = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
        if (endpoint.isBlank() || accessKeyId.isBlank() || secretAccessKey.isBlank() || bucket.isBlank()) {
            this.client = null;
            this.presigner = null;
            log.info("R2 sin configurar (faltan variables R2_*); backend R2 inactivo.");
            return;
        }
        var creds = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKeyId, secretAccessKey));
        this.client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(R2_REGION)
                .credentialsProvider(creds)
                .build();
        this.presigner = S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(R2_REGION)
                .credentialsProvider(creds)
                .build();
        log.info("R2 configurado: bucket '{}'.", bucket);
    }

    /** true si R2 tiene credenciales; StorageService lo comprueba al elegir backend. */
    public boolean isConfigured() {
        return client != null;
    }

    private void ensureReady() {
        if (client == null) {
            throw new IllegalStateException(
                    "STORAGE_BACKEND=r2 pero faltan variables R2_* (endpoint/claves/bucket).");
        }
    }

    @Override
    public void upload(String path, byte[] bytes, String contentType) {
        ensureReady();
        client.putObject(
                PutObjectRequest.builder().bucket(bucket).key(path)
                        .contentType(contentType).build(),
                RequestBody.fromBytes(bytes));
    }

    @Override
    public void delete(String path) {
        ensureReady();
        client.deleteObject(b -> b.bucket(bucket).key(path));
    }

    @Override
    public URL signedReadUrl(String path, int minutesValid) {
        // Las fotos "Tipo A" son públicas de por sí (ver PhotoController):
        // si hay un Public Development URL / dominio propio, se sirve por ahí
        // (CDN normal de Cloudflare, fiable para tráfico público) en vez de
        // por el endpoint S3 (pensado para llamadas servidor-a-servidor; los
        // móviles de los usuarios no siempre pueden conectar con él, ver
        // HISTORIAL 2026-08-15 — fotos que no cargaban pese a existir en R2).
        if (!publicUrl.isBlank()) {
            try {
                return URI.create(publicUrl + "/" + path).toURL();
            } catch (Exception e) {
                throw new IllegalArgumentException("R2_PUBLIC_URL o path inválido: " + path, e);
            }
        }
        ensureReady();
        GetObjectRequest get = GetObjectRequest.builder().bucket(bucket).key(path).build();
        GetObjectPresignRequest presign = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(minutesValid))
                .getObjectRequest(get)
                .build();
        return presigner.presignGetObject(presign).url();
    }

    @Override
    public byte[] readBytes(String path) {
        ensureReady();
        try {
            ResponseBytes<?> resp = client.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket).key(path).build());
            return resp.asByteArray();
        } catch (NoSuchKeyException e) {
            return null;
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            // R2 puede devolver el "no existe" como S3Exception 404 en vez de
            // NoSuchKeyException → tratarlo como null (no encontrado), no como error.
            if (e.statusCode() == 404) return null;
            throw e;
        }
    }

    @Override
    public List<String> listAll() {
        ensureReady();
        List<String> keys = new ArrayList<>();
        String token = null;
        do {
            ListObjectsV2Request.Builder req = ListObjectsV2Request.builder().bucket(bucket);
            if (token != null) req.continuationToken(token);
            var resp = client.listObjectsV2(req.build());
            for (S3Object o : resp.contents()) keys.add(o.key());
            token = resp.isTruncated() ? resp.nextContinuationToken() : null;
        } while (token != null);
        return keys;
    }
}
