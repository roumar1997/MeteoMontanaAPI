package com.meteomontana.api.infrastructure.geo;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Geocodificación de localidades (pueblo → coordenadas) vía Nominatim
 * (OpenStreetMap). El backend hace de proxy: una sola identidad (User-Agent),
 * resultados cacheados (caché "geocode", TTL del Caffeine global) y filtrado a
 * España, para no abusar del servicio de Nominatim ni exponer la IP del usuario.
 */
@Service
public class NominatimGeocoder {

    private static final Logger log = LoggerFactory.getLogger(NominatimGeocoder.class);

    private final RestClient client = RestClient.builder()
            .baseUrl("https://nominatim.openstreetmap.org")
            // Nominatim exige un User-Agent identificable con contacto.
            .defaultHeader("User-Agent", "Cumbre/1.0 (soporte@climbingteams.com)")
            .build();

    public record Place(String name, double lat, double lon) {}

    private record NominatimItem(
            @JsonProperty("display_name") String displayName,
            String lat,
            String lon) {}

    @Cacheable("geocode")
    public List<Place> search(String q) {
        if (q == null || q.isBlank()) return List.of();
        try {
            NominatimItem[] items = client.get()
                    .uri(uri -> uri.path("/search")
                            .queryParam("q", q)
                            .queryParam("format", "json")
                            .queryParam("limit", 6)
                            .queryParam("addressdetails", 0)
                            .queryParam("countrycodes", "es")
                            .queryParam("accept-language", "es")
                            .build())
                    .retrieve()
                    .body(NominatimItem[].class);
            if (items == null) return List.of();
            List<Place> out = new ArrayList<>();
            for (NominatimItem it : items) {
                try {
                    out.add(new Place(it.displayName(),
                            Double.parseDouble(it.lat()),
                            Double.parseDouble(it.lon())));
                } catch (Exception ignore) { /* coords inválidas → saltar */ }
            }
            return out;
        } catch (Exception e) {
            log.warn("geocode '{}' falló: {}", q, e.toString());
            return List.of();
        }
    }
}
