package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.mountain.MountainBulletinService;
import com.meteomontana.api.infrastructure.mountain.MountainAreas;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Boletín de montaña de AEMET (público, como el forecast).
 *
 * GET /api/mountain/bulletin?lat=..&lon=..[&day=0]
 *   → 200 con el boletín si las coordenadas caen en uno de los 9 macizos
 *   → 204 si no hay macizo (la app simplemente no pinta la sección)
 */
@RestController
@RequestMapping("/api/mountain")
public class MountainController {

    private final MountainBulletinService service;

    public MountainController(MountainBulletinService service) {
        this.service = service;
    }

    @GetMapping("/bulletin")
    public ResponseEntity<Map<String, Object>> bulletin(@RequestParam double lat,
                                                        @RequestParam double lon,
                                                        @RequestParam(defaultValue = "0") int day) {
        MountainAreas.Area area = service.areaFor(lat, lon);
        if (area == null) return ResponseEntity.noContent().build();
        Map<String, Object> b = service.bulletin(area.code(), Math.min(Math.max(day, 0), 2));
        if (b == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(b);
    }
}
