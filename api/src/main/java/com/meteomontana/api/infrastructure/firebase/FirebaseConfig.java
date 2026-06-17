package com.meteomontana.api.infrastructure.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Inicializa Firebase Admin SDK.
 *
 * Estrategia de credenciales (en orden de prioridad):
 *  1. Variable de entorno FIREBASE_SA_JSON con el contenido del JSON.
 *     Útil en producción (Railway/Render/Fly) para no subir el fichero a git.
 *  2. Fichero en resources/serviceAccountKey.json. Útil para desarrollo local.
 *
 * El bucket de Storage también es configurable por env var FIREBASE_STORAGE_BUCKET.
 */
@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);
    private static final String DEFAULT_BUCKET = "climbingteams.firebasestorage.app";

    @Value("${FIREBASE_SA_JSON:}")
    private String serviceAccountJson;

    @Value("${FIREBASE_STORAGE_BUCKET:" + DEFAULT_BUCKET + "}")
    private String storageBucket;

    @PostConstruct
    public void initialize() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) return;

        InputStream credentialsStream = resolveCredentials();
        if (credentialsStream == null) {
            log.error("No se encontraron credenciales Firebase. Configura FIREBASE_SA_JSON " +
                "o pon serviceAccountKey.json en resources/.");
            throw new IllegalStateException("Firebase credentials missing");
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                .setStorageBucket(storageBucket)
                .build();

        FirebaseApp.initializeApp(options);
        log.info("Firebase Admin SDK inicializado (bucket={})", storageBucket);
    }

    /**
     * Cliente Firestore para que el backend pueda leer el chat (que vive en
     * Firestore). Se usa, p.ej., para comprobar si una conversación ya existe
     * antes de decidir si mandar una push notification. Se inicializa de forma
     * perezosa tras {@link #initialize()} (FirebaseApp ya estará listo cuando
     * Spring resuelva este bean, porque @PostConstruct corre antes).
     */
    @Bean
    public Firestore firestore() {
        return FirestoreClient.getFirestore();
    }

    private InputStream resolveCredentials() {
        // 1) Env var con el JSON entero (producción).
        if (serviceAccountJson != null && !serviceAccountJson.isBlank()) {
            log.info("Cargando credenciales Firebase desde env var FIREBASE_SA_JSON");
            return new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8));
        }
        // 2) Fichero local (desarrollo).
        InputStream file = getClass().getClassLoader().getResourceAsStream("serviceAccountKey.json");
        if (file != null) {
            log.info("Cargando credenciales Firebase desde resources/serviceAccountKey.json");
        }
        return file;
    }
}
