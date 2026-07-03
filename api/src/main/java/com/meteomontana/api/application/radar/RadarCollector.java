package com.meteomontana.api.application.radar;

import com.meteomontana.api.infrastructure.radar.AemetRadarClient;
import com.meteomontana.api.infrastructure.radar.RadarFrameEntity;
import com.meteomontana.api.infrastructure.radar.SpringDataRadarFrameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;

/**
 * Recolector del radar de AEMET.
 *
 * AEMET solo sirve la imagen más reciente de cada radar (cadencia 10 min),
 * así que este cron descarga los 15 radares regionales cada 10 minutos y los
 * archiva en radar_frames. Con eso construimos nosotros la "película" de las
 * últimas horas que anima la pestaña Radar de las apps.
 *
 * Detalles finos:
 * - AEMET tiene rate limit por minuto → espaciamos ~2,5s entre radares.
 * - Si AEMET aún no renovó la imagen, el sha256 coincide con el último frame
 *   guardado y NO duplicamos (la timeline no debe repetir frames).
 * - captured_at se redondea al múltiplo de 10 min del ciclo, así todos los
 *   radares de una misma vuelta comparten timestamp y las apps pueden pedir
 *   "el frame de las 18:40" de España entera.
 * - Retención: 6h (la UI enseña 2h por defecto con opción de 6h).
 */
@Component
public class RadarCollector {

    private static final Logger log = LoggerFactory.getLogger(RadarCollector.class);

    /** Los 15 radares regionales de AEMET (códigos oficiales de OpenData). */
    static final List<String> RADAR_CODES = List.of(
            "am", "sa", "pm", "ba", "cc", "co", "ma", "ml",
            "mu", "vd", "ca", "se", "va", "ss", "za");

    private static final long PAUSE_BETWEEN_RADARS_MS = 2_500;
    private static final int RETENTION_HOURS = 6;

    private final AemetRadarClient client;
    private final SpringDataRadarFrameRepository repo;

    public RadarCollector(AemetRadarClient client, SpringDataRadarFrameRepository repo) {
        this.client = client;
        this.repo = repo;
    }

    /** Cada 10 min, alineado al reloj (00, 10, 20...), 1 min de gracia para AEMET. */
    @Scheduled(cron = "0 1/10 * * * *", zone = "Europe/Madrid")
    public void collect() {
        if (!client.isConfigured()) {
            return; // sin AEMET_API_KEY (p.ej. local) el recolector queda dormido
        }
        LocalDateTime cycle = LocalDateTime.now(java.time.ZoneId.of("Europe/Madrid"))
                .truncatedTo(ChronoUnit.MINUTES);
        cycle = cycle.minusMinutes(cycle.getMinute() % 10); // 18:41 → 18:40

        int saved = 0, skipped = 0;
        for (String code : RADAR_CODES) {
            try {
                byte[] image = client.fetchRegional(code).orElse(null);
                if (image != null && saveIfNew(code, cycle, image)) saved++;
                else skipped++;
                Thread.sleep(PAUSE_BETWEEN_RADARS_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.warn("Radar {}: fallo inesperado: {}", code, e.getMessage());
                skipped++;
            }
        }
        int pruned = prune();
        log.info("Radar AEMET: {} frames nuevos, {} sin cambio/sin dato, {} purgados (ciclo {})",
                saved, skipped, pruned, cycle);
    }

    @Transactional
    protected boolean saveIfNew(String code, LocalDateTime cycle, byte[] image) {
        String sha = sha256(image);
        // Mismo contenido que el último frame → AEMET aún no renovó; no duplicar.
        boolean unchanged = repo.findTopByRadarCodeOrderByCapturedAtDesc(code)
                .map(last -> last.getSha256().equals(sha))
                .orElse(false);
        if (unchanged || repo.findByRadarCodeAndCapturedAt(code, cycle).isPresent()) {
            return false;
        }
        repo.save(new RadarFrameEntity(code, cycle, image, sha));
        return true;
    }

    @Transactional
    protected int prune() {
        return repo.deleteOlderThan(LocalDateTime.now().minusHours(RETENTION_HOURS));
    }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
