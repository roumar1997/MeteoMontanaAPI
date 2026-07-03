package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.radar.RadarService;
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
import java.util.Map;

/**
 * API pública del radar de lluvia (datos AEMET cocinados por el backend).
 *
 * Las apps SOLO hablan con estos endpoints — nunca con AEMET directamente —
 * así la fuente es intercambiable sin actualizar la app.
 *
 * - GET /api/radar/frames?radar=ma&hours=2 → timeline + esquinas geográficas.
 * - GET /api/radar/frame/{radar}/{ts}      → PNG transparente en paleta Cumbre.
 */
@RestController
@RequestMapping("/api/radar")
public class RadarController {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    private final RadarService service;

    public RadarController(RadarService service) {
        this.service = service;
    }

    @GetMapping("/frames")
    public Map<String, Object> frames(@RequestParam(defaultValue = "2") int hours,
                                      @RequestParam(required = false) String radar,
                                      @RequestParam(required = false) Double lat,
                                      @RequestParam(required = false) Double lon) {
        // Por defecto: compuesto España entera ("es"). Con lat/lon se puede
        // pedir el radar regional más cercano (mejor resolución); la tabla de
        // antenas vive solo en el backend.
        if (radar == null) {
            radar = (lat != null && lon != null)
                    ? com.meteomontana.api.infrastructure.radar.RadarSites.nearest(lat, lon).code()
                    : com.meteomontana.api.application.radar.RadarComposite.CODE;
        }
        double[] b = service.bounds(radar);
        var timeline = com.meteomontana.api.application.radar.RadarComposite.CODE.equals(radar)
                ? service.compositeTimeline(hours)
                : service.timeline(radar, hours).stream()
                        .map(com.meteomontana.api.infrastructure.radar.RadarFrameEntity::getCapturedAt)
                        .toList();
        return Map.of(
                "radar", radar,
                // Esquinas del PNG para clavarlo en el mapa (MapLibre ImageSource).
                "bounds", Map.of("north", b[0], "west", b[1], "south", b[2], "east", b[3]),
                "frames", timeline.stream().map(t -> Map.of(
                        "ts", t.format(TS),
                        "capturedAt", t.toString())).toList());
    }

    @GetMapping("/frame/{radar}/{ts}")
    public ResponseEntity<byte[]> frame(@PathVariable String radar, @PathVariable String ts) {
        LocalDateTime at = LocalDateTime.parse(ts, TS);
        byte[] png = com.meteomontana.api.application.radar.RadarComposite.CODE.equals(radar)
                ? service.renderedComposite(at)
                : service.renderedFrame(radar, at);
        return ResponseEntity.ok()
                // El PNG de un frame es estable → las apps pueden cachearlo un rato
                // largo (si la máscara mejora, el ts nuevo ya viene distinto).
                .cacheControl(CacheControl.maxAge(Duration.ofHours(2)).cachePublic())
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }
}
