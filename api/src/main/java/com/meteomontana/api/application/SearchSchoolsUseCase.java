package com.meteomontana.api.application;

import com.meteomontana.api.domain.model.School;
import com.meteomontana.api.domain.port.SchoolRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * Búsqueda incremental para autocomplete.
 * Acentos-insensitive y case-insensitive.
 */
@Service
@RequiredArgsConstructor
public class SearchSchoolsUseCase {

    private final SchoolRepository repository;

    public List<School> execute(String query, int limit) {
        if (query == null || query.trim().isBlank()) return List.of();
        String needle = normalize(query.trim());
        return repository.findAll().stream()
                .filter(s -> {
                    String haystack = normalize(s.getName() + " " + s.getLocation() + " " + s.getRegion());
                    return haystack.contains(needle);
                })
                .limit(limit > 0 ? limit : 10)
                .toList();
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return n.toLowerCase();
    }
}
