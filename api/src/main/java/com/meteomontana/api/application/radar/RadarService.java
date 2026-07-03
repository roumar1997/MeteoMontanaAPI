package com.meteomontana.api.application.radar;

import com.meteomontana.api.infrastructure.radar.RadarFrameEntity;
import com.meteomontana.api.infrastructure.radar.RadarSites;
import com.meteomontana.api.infrastructure.radar.SpringDataRadarFrameRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/** Casos de uso del radar: timeline y frame repintado en Cumbre. */
@Service
public class RadarService {

    private final SpringDataRadarFrameRepository repo;
    private final RadarCumbreRenderer renderer;

    public RadarService(SpringDataRadarFrameRepository repo, RadarCumbreRenderer renderer) {
        this.repo = repo;
        this.renderer = renderer;
    }

    @Transactional(readOnly = true)
    public List<RadarFrameEntity> timeline(String radar, int hours) {
        int h = Math.min(Math.max(hours, 1), 6);
        return repo.findByRadarCodeAndCapturedAtAfterOrderByCapturedAtAsc(
                radar, nowMadrid().minus(Duration.ofHours(h)));
    }

    /**
     * PNG Cumbre de un frame. Un frame archivado nunca cambia, pero su máscara
     * de estáticos mejora con los frames siguientes; la caché corta (TTL global
     * de Caffeine) da el equilibrio: rápido de servir y se re-renderiza a rato.
     */
    @Cacheable(cacheNames = "radar-png", key = "#radar + '/' + #capturedAt")
    @Transactional(readOnly = true)
    public byte[] renderedFrame(String radar, LocalDateTime capturedAt) {
        RadarFrameEntity frame = repo.findByRadarCodeAndCapturedAt(radar, capturedAt)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "frame no archivado"));
        // Últimas ~2h del mismo radar como referencia de "qué no se mueve".
        List<byte[]> recent = repo
                .findByRadarCodeAndCapturedAtAfterOrderByCapturedAtAsc(
                        radar, capturedAt.minusHours(2))
                .stream()
                .filter(f -> !f.getCapturedAt().equals(capturedAt))
                .map(RadarFrameEntity::getImage)
                .toList();
        try {
            return renderer.render(frame.getImage(), recent);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "frame ilegible");
        }
    }

    /** Los frames se archivan con hora de Madrid; el servidor corre en UTC.
     *  TODAS las ventanas temporales deben usar la MISMA zona o desaparecen
     *  las últimas 2h (bug visto en producción el día del estreno). */
    private static LocalDateTime nowMadrid() {
        return LocalDateTime.now(java.time.ZoneId.of("Europe/Madrid"));
    }

    public double[] bounds(String radar) {
        if (RadarComposite.CODE.equals(radar)) return RadarComposite.bounds();
        double[] b = RadarSites.bounds(radar);
        if (b == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "radar desconocido");
        return b;
    }

    /** Ciclos disponibles para el compuesto España (cualquier radar cuenta). */
    @Transactional(readOnly = true)
    public List<LocalDateTime> compositeTimeline(int hours) {
        int h = Math.min(Math.max(hours, 1), 48);
        LocalDateTime now = nowMadrid();
        return repo.findDistinctCapturedAtBetween(now.minus(Duration.ofHours(h)), now.plusMinutes(5));
    }

    /** Ciclos del compuesto en un día concreto (chips HOY/AYER de la app). */
    @Transactional(readOnly = true)
    public List<LocalDateTime> compositeTimelineForDay(java.time.LocalDate day) {
        return repo.findDistinctCapturedAtBetween(
                day.atStartOfDay(), day.plusDays(1).atStartOfDay());
    }

    /**
     * PNG del compuesto Península+Baleares en un ciclo: por cada radar se usa
     * su último frame en o antes del ciclo (el dedupe del recolector hace que
     * un radar sin cambios no re-archive) y se cose al lienzo común.
     */
    @Cacheable(cacheNames = "radar-png", key = "'es/' + #capturedAt")
    @Transactional(readOnly = true)
    public byte[] renderedComposite(LocalDateTime capturedAt) {
        java.awt.image.BufferedImage canvas = new java.awt.image.BufferedImage(
                RadarComposite.WIDTH, RadarComposite.HEIGHT,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        boolean any = false;
        for (String code : RadarSites.SITES.keySet()) {
            if ("ca".equals(code)) continue; // Canarias: fuera del lienzo peninsular
            RadarFrameEntity frame = repo
                    .findTopByRadarCodeAndCapturedAtLessThanEqualOrderByCapturedAtDesc(code, capturedAt)
                    // dato más viejo de 40 min = radar caído: mejor hueco que lluvia fantasma
                    .filter(f -> !f.getCapturedAt().isBefore(capturedAt.minusMinutes(40)))
                    .orElse(null);
            if (frame == null) continue;
            List<byte[]> recent = repo
                    .findByRadarCodeAndCapturedAtAfterOrderByCapturedAtAsc(
                            code, frame.getCapturedAt().minusHours(2))
                    .stream()
                    .filter(f -> !f.getCapturedAt().equals(frame.getCapturedAt()))
                    .map(RadarFrameEntity::getImage)
                    .toList();
            try {
                RadarComposite.paste(canvas, renderer.renderImage(frame.getImage(), recent), code);
                any = true;
            } catch (IOException e) { /* frame ilegible: ese radar no pinta */ }
        }
        if (!any) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "sin datos en ese ciclo");
        RadarCumbreRenderer.dilate(canvas);
        try {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream(64 * 1024);
            javax.imageio.ImageIO.write(canvas, "png", bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "error al componer");
        }
    }
}
