package com.meteomontana.api.infrastructure.mountain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Boletín de montaña de AEMET OpenData (dos pasos, como el radar: JSON con
 * URL temporal → dato real). El dato llega en Latin-1, no UTF-8 — de ahí la
 * decodificación explícita. Misma key que el radar (AEMET_API_KEY).
 */
@Component
public class AemetMountainClient {

    private static final Logger log = LoggerFactory.getLogger(AemetMountainClient.class);
    private static final String BASE_URL = "https://opendata.aemet.es/opendata";

    private final RestClient restClient;
    private final ObjectMapper mapper;
    private final String apiKey;

    public AemetMountainClient(ObjectMapper mapper, @Value("${aemet.api-key:}") String apiKey) {
        this.mapper = mapper;
        this.apiKey = apiKey;
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Duration.ofSeconds(10));
        f.setReadTimeout(Duration.ofSeconds(30));
        this.restClient = RestClient.builder().requestFactory(f).build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** Boletín del área ('mad2'...) para el día (0=hoy, 1=mañana). */
    public Optional<JsonNode> fetchBulletin(String areaCode, int day) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> meta = restClient.get()
                    .uri(BASE_URL + "/api/prediccion/especifica/montaña/pasada/area/{a}/dia/{d}",
                            areaCode, day)
                    .header("api_key", apiKey)
                    .retrieve()
                    .body(Map.class);
            if (meta == null || !(meta.get("datos") instanceof String datosUrl)) {
                return Optional.empty();
            }
            byte[] raw = restClient.get().uri(datosUrl).retrieve().body(byte[].class);
            if (raw == null || raw.length == 0) return Optional.empty();
            String json = new String(raw, StandardCharsets.ISO_8859_1);
            return Optional.of(mapper.readTree(json));
        } catch (Exception e) {
            log.debug("Boletín AEMET no disponible ({} d{}): {}", areaCode, day, e.getMessage());
            return Optional.empty();
        }
    }
}
