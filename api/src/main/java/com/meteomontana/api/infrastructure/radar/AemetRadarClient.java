package com.meteomontana.api.infrastructure.radar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Cliente de la API OpenData de AEMET para las imágenes de radar.
 *
 * AEMET responde en dos pasos: la petición autenticada devuelve un JSON con
 * una URL temporal en "datos", y esa segunda URL sirve el GIF real.
 * La API key va en la cabecera "api_key" (variable de entorno AEMET_API_KEY,
 * nunca en el código). Rate limit estricto por minuto: el recolector espacia
 * las peticiones y aquí tratamos 429/404 como "sin dato ahora", no como error.
 */
@Component
public class AemetRadarClient {

    private static final Logger log = LoggerFactory.getLogger(AemetRadarClient.class);

    private static final String BASE_URL = "https://opendata.aemet.es/opendata";

    private final RestClient restClient;
    private final String apiKey;

    public AemetRadarClient(@Value("${aemet.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Duration.ofSeconds(10));
        f.setReadTimeout(Duration.ofSeconds(30));
        this.restClient = RestClient.builder().requestFactory(f).build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** Imagen del radar regional (código 'ma', 'za'...). Empty si AEMET no tiene dato ahora. */
    public Optional<byte[]> fetchRegional(String radarCode) {
        return fetch(BASE_URL + "/api/red/radar/regional/" + radarCode);
    }

    /** Composición nacional. AEMET la sirve a ratos (404 frecuentes); es opcional. */
    public Optional<byte[]> fetchNacional() {
        return fetch(BASE_URL + "/api/red/radar/nacional");
    }

    private Optional<byte[]> fetch(String url) {
        try {
            // Paso 1: JSON con la URL temporal del dato.
            @SuppressWarnings("unchecked")
            Map<String, Object> meta = restClient.get()
                    .uri(url)
                    .header("api_key", apiKey)
                    .retrieve()
                    .body(Map.class);
            if (meta == null || !(meta.get("datos") instanceof String datosUrl)) {
                log.info("AEMET sin dato en {}: {}", url, meta);
                return Optional.empty();
            }
            // Paso 2: el GIF real (la URL temporal no requiere key).
            byte[] image = restClient.get().uri(datosUrl).retrieve().body(byte[].class);
            if (image == null || image.length < 100) return Optional.empty();
            return Optional.of(image);
        } catch (Exception e) {
            // 404 = "sin datos", 429 = rate limit. En INFO: tragarlo en debug
            // nos dejó ciegos cuando prod y staging empezaron a pisarse la key.
            log.info("AEMET no disponible ({}): {}", url, e.getMessage());
            return Optional.empty();
        }
    }
}
