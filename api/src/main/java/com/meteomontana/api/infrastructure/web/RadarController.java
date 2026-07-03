package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.infrastructure.radar.RadarFrameEntity;
import com.meteomontana.api.infrastructure.radar.SpringDataRadarFrameRepository;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * API pública del radar de lluvia (datos AEMET archivados por RadarCollector).
 *
 * Las apps SOLO hablan con estos endpoints — nunca con AEMET directamente —
 * así la fuente es intercambiable sin actualizar la app.
 *
 * Día 2 (pendiente): /frame servirá el PNG repintado en paleta Cumbre y
 * georreferenciado; de momento sirve el GIF crudo para validar el pipeline.
 */
@RestController
@RequestMapping("/api/radar")
public class RadarController {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    private final SpringDataRadarFrameRepository repo;

    public RadarController(SpringDataRadarFrameRepository repo) {
        this.repo = repo;
    }

    /** Timeline: instantes disponibles por radar en las últimas {hours} horas (máx 6). */
    @GetMapping("/frames")
    public Map<String, Object> frames(@RequestParam(defaultValue = "2") int hours,
                                      @RequestParam(defaultValue = "ma") String radar) {
        int h = Math.min(Math.max(hours, 1), 6);
        List<RadarFrameEntity> list = repo
                .findByRadarCodeAndCapturedAtAfterOrderByCapturedAtAsc(
                        radar, LocalDateTime.now().minus(Duration.ofHours(h)));
        return Map.of(
                "radar", radar,
                "frames", list.stream().map(f -> Map.of(
                        "ts", f.getCapturedAt().format(TS),
                        "capturedAt", f.getCapturedAt().toString())).toList());
    }

    /** Imagen de un frame concreto (ts con formato yyyyMMdd-HHmm). */
    @GetMapping("/frame/{radar}/{ts}")
    public ResponseEntity<byte[]> frame(@PathVariable String radar, @PathVariable String ts) {
        LocalDateTime capturedAt = LocalDateTime.parse(ts, TS);
        return repo.findByRadarCodeAndCapturedAt(radar, capturedAt)
                .map(f -> ResponseEntity.ok()
                        // Un frame nunca cambia una vez archivado → cache larga.
                        .cacheControl(CacheControl.maxAge(Duration.ofHours(12)).cachePublic())
                        .contentType(MediaType.IMAGE_GIF)
                        .body(f.getImage()))
                .orElse(ResponseEntity.notFound().build());
    }
}
