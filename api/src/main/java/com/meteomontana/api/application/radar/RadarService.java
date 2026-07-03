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
                radar, LocalDateTime.now().minus(Duration.ofHours(h)));
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

    public double[] bounds(String radar) {
        double[] b = RadarSites.bounds(radar);
        if (b == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "radar desconocido");
        return b;
    }
}
