package com.meteomontana.api.application.mountain;

import com.fasterxml.jackson.databind.JsonNode;
import com.meteomontana.api.infrastructure.mountain.AemetMountainClient;
import com.meteomontana.api.infrastructure.mountain.MountainAreas;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * Boletín de montaña listo para la app: localiza el macizo de unas
 * coordenadas y aplana el JSON navideño de AEMET a un objeto simple.
 *
 * Estructura de AEMET: array raíz → "seccion"[] → cada una con "apartado"[]
 * (cabecera/texto) y/o "lugar"[] (puntos con altitud y temperaturas).
 */
@Service
@RequiredArgsConstructor
public class MountainBulletinService {

    private final AemetMountainClient client;

    /** Null si las coordenadas no caen en ningún macizo o AEMET no responde. */
    @Cacheable(cacheNames = "mountain-bulletin", key = "#areaCode + '/' + #day")
    public Map<String, Object> bulletin(String areaCode, int day) {
        MountainAreas.Area area = MountainAreas.AREAS.stream()
                .filter(a -> a.code().equals(areaCode)).findFirst().orElse(null);
        if (area == null || !client.isConfigured()) return null;
        JsonNode root = client.fetchBulletin(areaCode, day).orElse(null);
        if (root == null || !root.isArray() || root.isEmpty()) return null;

        Map<String, String> texts = new LinkedHashMap<>();
        List<Map<String, Object>> spots = new ArrayList<>();
        for (JsonNode seccion : root.get(0).path("seccion")) {
            for (JsonNode ap : seccion.path("apartado")) {
                String nombre = ap.path("nombre").asText("");
                String texto = ap.path("texto").asText("");
                if (!nombre.isEmpty() && !texto.isEmpty()) texts.put(nombre, texto);
            }
            for (JsonNode lugar : seccion.path("lugar")) {
                if (!lugar.hasNonNull("nombre")) continue;
                Map<String, Object> spot = new LinkedHashMap<>();
                spot.put("nombre", lugar.path("nombre").asText());
                spot.put("altitud", lugar.path("altitud").asText(""));
                spot.put("minima", lugar.path("minima").asInt());
                spot.put("maxima", lugar.path("maxima").asInt());
                spots.add(spot);
            }
        }
        if (texts.isEmpty()) return null;

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("area", area.code());
        out.put("areaName", area.name());
        out.put("day", day);
        // Los 5 textos del boletín (nubosidad, pcp, tormentas, temperatura,
        // viento) + atmósfera libre (isocero, iso10, v1500, v3000).
        out.put("texts", texts);
        out.put("spots", spots);
        return out;
    }

    /** Área para unas coordenadas (o null). */
    public MountainAreas.Area areaFor(double lat, double lon) {
        return MountainAreas.forLocation(lat, lon);
    }
}
