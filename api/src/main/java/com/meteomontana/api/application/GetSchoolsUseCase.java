package com.meteomontana.api.application;

import com.meteomontana.api.domain.model.School;
import com.meteomontana.api.domain.port.SchoolRepository;
import com.meteomontana.api.domain.util.GeoDistance;
import org.springframework.stereotype.Service;

import java.util.List;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetSchoolsUseCase {

    private final SchoolRepository repository;

    public List<School> execute(String region, String style, List<String> rockTypes,
                                Double lat, Double lon, Double radioKm) {
        boolean distanceFilterActive = lat != null && lon != null && radioKm != null;
        boolean rockFilterActive = rockTypes != null && !rockTypes.isEmpty();

        return repository.findAll().stream()
                .filter(s -> region == null || region.isBlank()
                          || region.equalsIgnoreCase(s.getRegion()))
                .filter(s -> style == null || style.isBlank()
                          || style.equalsIgnoreCase(s.getStyle()))
                .filter(s -> !rockFilterActive
                          || rockTypes.stream().anyMatch(r -> r.equalsIgnoreCase(s.getRockType())))
                .filter(s -> !distanceFilterActive
                          || GeoDistance.haversineKM(lat, lon, s.getLat(), s.getLon()) <= radioKm)
                .toList();
    }
}
