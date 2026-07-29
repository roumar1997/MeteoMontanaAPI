package com.meteomontana.api.application.search;

import com.meteomontana.api.domain.model.LineSearchHit;
import com.meteomontana.api.domain.port.LineSearchRepository;
import com.meteomontana.api.domain.port.SchoolRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * Búsqueda GLOBAL de vías y bloques por nombre (buscador de la pantalla de
 * Escuelas). Vías primero, piedras después, cada resultado con su escuela
 * resuelta y con foto+trazo para pintar el mini-topo en las sugerencias.
 */
@Service
@RequiredArgsConstructor
public class SearchLinesService {

    private static final int MAX_LINES = 15;
    private static final int MAX_BLOCKS = 10;
    private static final int MAX_TOTAL = 20;

    private final LineSearchRepository search;
    private final SchoolRepository schools;

    public List<LineSearchHit> search(String q) {
        String query = q == null ? "" : q.trim();
        if (query.length() < 2) return List.of();

        List<LineSearchHit> out = new ArrayList<>();
        out.addAll(search.searchLinesByName(query, MAX_LINES));
        for (LineSearchHit b : search.searchBlocksByName(query, MAX_BLOCKS)) {
            // "1", "2"… son piedras autonumeradas y ensuciarían la búsqueda.
            if (b.blockName() != null && b.blockName().matches("\\d+")) continue;
            out.add(b);
        }
        if (out.size() > MAX_TOTAL) out = out.subList(0, MAX_TOTAL);

        // Nombre de escuela resuelto UNA vez por escuela distinta.
        Map<String, String> names = new HashMap<>();
        return out.stream().map(h -> h.withSchoolName(
                names.computeIfAbsent(h.schoolId(),
                        id -> schools.findById(id).map(s -> s.getName()).orElse(""))))
                .toList();
    }
}
