package com.meteomontana.tools;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Script de migración: lee notas de Firestore (sectores/{sectorId}/notas/{noteId})
 * y las inserta en la tabla notes de Postgres.
 *
 * Uso: java -jar migrate-notes.jar <ruta-serviceAccountKey.json> <postgres-password>
 */
public class MigrateNotes {

    // Cambia estos valores si tu setup es distinto
    private static final String DB_URL  = "jdbc:postgresql://localhost:5432/meteomontana";
    private static final String DB_USER = "meteomontana";

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Uso: java -jar migrate-notes.jar <serviceAccountKey.json> <postgres-password>");
            System.exit(1);
        }

        String keyPath  = args[0];
        String dbPass   = args[1];

        // 1. Inicializar Firebase Admin SDK
        System.out.println("Conectando a Firebase...");
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(new FileInputStream(keyPath)))
                .build();
        FirebaseApp.initializeApp(options);
        Firestore db = FirestoreClient.getFirestore();

        // 2. Conectar a Postgres
        System.out.println("Conectando a Postgres...");
        Connection conn = DriverManager.getConnection(DB_URL, DB_USER, dbPass);
        conn.setAutoCommit(false); // transacción manual: o todo o nada

        int inserted = 0;
        int skipped  = 0;

        // 3. Leer todas las notas de todos los sectores con collectionGroup
        // Esto funciona aunque los documentos padre (sectores) no existan como documentos reales
        System.out.println("Leyendo notas de Firestore (collectionGroup)...");
        List<QueryDocumentSnapshot> notas = db.collectionGroup("notas").get().get().getDocuments();
        System.out.println("Notas encontradas: " + notas.size());

        for (QueryDocumentSnapshot nota : notas) {
            String noteId = nota.getId();

            // El path de una nota es: sectores/{sectorId}/notas/{noteId}
            // nota.getReference().getParent() → colección "notas"
            // .getParent() → documento del sector
            String sectorId = nota.getReference().getParent().getParent().getId();

            // Convertir el id del sector a lat/lon
            double[] coords = parseSectorId(sectorId);
            if (coords == null) {
                System.out.println("  [SKIP] No se pudo parsear sectorId: " + sectorId);
                skipped++;
                continue;
            }

            double sectorLat = coords[0];
            double sectorLon = coords[1];

            // Buscar la escuela más cercana en Postgres
            String schoolId = findClosestSchool(conn, sectorLat, sectorLon);
            if (schoolId == null) {
                System.out.println("  [SKIP] No se encontró escuela cercana para sector: " + sectorId
                        + " (lat=" + sectorLat + ", lon=" + sectorLon + ")");
                skipped++;
                continue;
            }

            String texto = nota.getString("texto");
            String autor = nota.getString("autor");
            String uid   = nota.getString("uid");
            Date   fecha = nota.getDate("fecha");

            if (texto == null || autor == null || uid == null) {
                System.out.println("  [SKIP] Nota incompleta: " + noteId);
                skipped++;
                continue;
            }

            // Contar votos desde los arrays (pueden ser null si están vacíos)
            List<?> votosUp   = (List<?>) nota.get("votantesUp");
            List<?> votosDown = (List<?>) nota.get("votantesDown");
            int upvotes   = votosUp   != null ? votosUp.size()   : 0;
            int downvotes = votosDown != null ? votosDown.size() : 0;

            // Insertar en Postgres (idempotente: ignora si ya existe)
            if (noteExists(conn, noteId)) {
                System.out.println("  [SKIP] Nota ya existe: " + noteId);
                skipped++;
                continue;
            }

            insertNote(conn, noteId, schoolId, texto, autor, uid, fecha, upvotes, downvotes);
            System.out.println("  [OK] " + noteId + " → escuela: " + schoolId + " (sector: " + sectorId + ")");
            inserted++;
        }

        conn.commit();
        conn.close();

        System.out.println("\n--- Migración completada ---");
        System.out.println("Insertadas: " + inserted);
        System.out.println("Saltadas:   " + skipped);
    }

    /**
     * Parsea el id de Firestore "4039_-141" a [40.39, -1.41].
     * Devuelve null si el formato no es el esperado.
     */
    private static double[] parseSectorId(String sectorId) {
        try {
            String[] parts = sectorId.split("_(?=-?\\d)"); // split por _ seguido de dígito o -
            if (parts.length != 2) return null;
            double lat = Integer.parseInt(parts[0]) / 100.0;
            double lon = Integer.parseInt(parts[1]) / 100.0;
            return new double[]{lat, lon};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Busca en Postgres la escuela cuyas coordenadas estén más cerca de (lat, lon).
     * Devuelve null si la más cercana está a más de 5 km (umbral de seguridad).
     */
    private static String findClosestSchool(Connection conn, double lat, double lon) throws Exception {
        String sql = """
                SELECT id,
                       ( 6371 * acos(
                           cos(radians(?)) * cos(radians(lat)) *
                           cos(radians(lon) - radians(?)) +
                           sin(radians(?)) * sin(radians(lat))
                       )) AS distance_km
                FROM schools
                ORDER BY distance_km
                LIMIT 1
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, lat);
            ps.setDouble(2, lon);
            ps.setDouble(3, lat);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double distKm = rs.getDouble("distance_km");
                    if (distKm > 5.0) return null; // demasiado lejos, no es una coincidencia fiable
                    return rs.getString("id");
                }
            }
        }
        return null;
    }

    private static boolean noteExists(Connection conn, String noteId) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM notes WHERE id = ?")) {
            ps.setString(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static void insertNote(Connection conn, String noteId, String schoolId,
                                   String text, String author, String uid,
                                   Date fecha, int upvotes, int downvotes) throws Exception {
        String sql = """
                INSERT INTO notes (id, school_id, text, author, uid, created_at, upvotes_count, downvotes_count)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, noteId);
            ps.setString(2, schoolId);
            ps.setString(3, text);
            ps.setString(4, author);
            ps.setString(5, uid);
            ps.setTimestamp(6, fecha != null
                    ? new Timestamp(fecha.getTime())
                    : Timestamp.from(Instant.now()));
            ps.setInt(7, upvotes);
            ps.setInt(8, downvotes);
            ps.executeUpdate();
        }
    }
}
