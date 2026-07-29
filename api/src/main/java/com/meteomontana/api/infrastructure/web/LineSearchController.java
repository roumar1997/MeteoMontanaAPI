package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.search.SearchLinesService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * Búsqueda GLOBAL de vías y bloques (buscador de la pantalla de Escuelas).
 * Pública, como el catálogo. La lógica vive en {@link SearchLinesService};
 * aquí solo el mapeo HTTP. Los campos photoPath/linePath/startType son
 * ADITIVOS y nullable (las apps viejas los ignoran): permiten pintar el
 * mini-topo de cada resultado.
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class LineSearchController {

    public record LineHit(String schoolId, String schoolName,
                          String blockId, String blockName,
                          String lineId, String lineName, String grade,
                          String sectorName,
                          String photoPath, String linePath, String startType) {}

    private final SearchLinesService service;

    @GetMapping("/lines")
    public List<LineHit> search(@RequestParam String q) {
        return service.search(q).stream()
                .map(h -> new LineHit(h.schoolId(), h.schoolName(), h.blockId(),
                        h.blockName(), h.lineId(), h.lineName(), h.grade(),
                        h.sectorName(), h.photoPath(), h.linePath(), h.startType()))
                .toList();
    }
}
