package com.meteomontana.api.infrastructure.weather;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Cliente HTTP para llamar a la API de Open-Meteo.
 * Captura errores HTTP de Open-Meteo (ej. rate-limit 403/429) y los convierte
 * en ResponseStatusException 503 para no exponer el status interno al cliente.
 */
@Component
public class OpenMeteoClient {

    private static final Logger log = LoggerFactory.getLogger(OpenMeteoClient.class);

    private static final String BASE_URL = "https://api.open-meteo.com/v1/forecast";
    private static final String HOURLY_VARS =
            "temperature_2m,relative_humidity_2m,precipitation,"
                    + "precipitation_probability,wind_speed_10m,cloud_cover,dew_point_2m,weather_code";

    /** Máximo de localizaciones por petición batch (URL razonable; 326 → 4 calls). */
    private static final int BATCH_SIZE = 100;

    private final RestClient restClient;
    private final CacheManager cacheManager;

    public OpenMeteoClient(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        // Timeouts holgados: Open-Meteo responde lento cuando estrangula la IP
        // (Railway comparte IP de salida con otras apps).
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(25).toMillis());
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory(factory)
                .build();
    }

    /** Si Open-Meteo devuelve 429, dejamos de llamar hasta este instante (epoch ms). */
    private volatile long cooldownUntil = 0;

    @Cacheable(value = "forecast", key = "#lat + ',' + #lon")
    public OpenMeteoResponse fetchForecast(double lat, double lon) {
        if (System.currentTimeMillis() < cooldownUntil) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El servicio meteorológico está limitando las peticiones (429). "
                            + "Reintentando automáticamente en unos minutos.");
        }
        try {
            return doFetch(lat, lon);
        } catch (RateLimitedException e) {
            // 429: martillear solo lo empeora — paramos 10 min para que el límite se recupere.
            cooldownUntil = System.currentTimeMillis() + Duration.ofMinutes(10).toMillis();
            log.warn("Open-Meteo 429: cooldown de 10 min activado");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El servicio meteorológico está limitando las peticiones (429). "
                            + "Reintentando automáticamente en unos minutos.");
        } catch (ResponseStatusException first) {
            // Otros errores (timeout, corte de conexión) suelen ser intermitentes: un reintento.
            try { Thread.sleep(800); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            try {
                return doFetch(lat, lon);
            } catch (RateLimitedException e) {
                cooldownUntil = System.currentTimeMillis() + Duration.ofMinutes(10).toMillis();
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "El servicio meteorológico está limitando las peticiones (429).");
            }
        }
    }

    /**
     * Pre-calienta la caché de forecast para muchas coordenadas con POCAS
     * peticiones (Open-Meteo acepta varias localizaciones por petición y devuelve
     * un array). Evita el pico de cientos de llamadas simultáneas (p.ej. al cargar
     * los scores de todas las escuelas tras un redeploy con la caché vacía), que
     * es lo que disparaba el 429. Las que ya están en caché no se vuelven a pedir.
     * Best-effort: si Open-Meteo limita o falla, no propaga (cada escuela caerá a
     * su llamada individual / cooldown como antes).
     */
    public void prewarm(List<double[]> coords) {
        if (coords == null || coords.isEmpty()) return;
        if (System.currentTimeMillis() < cooldownUntil) return;   // ya limitados: no insistir

        // Solo las que NO están cacheadas (y sin duplicar coordenadas).
        List<double[]> missing = coords.stream()
                .filter(c -> getCached(c[0], c[1]) == null)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(c -> cacheKey(c[0], c[1]), c -> c, (a, b) -> a),
                        m -> new ArrayList<>(m.values())));
        if (missing.isEmpty()) return;

        for (int from = 0; from < missing.size(); from += BATCH_SIZE) {
            List<double[]> chunk = missing.subList(from, Math.min(from + BATCH_SIZE, missing.size()));
            try {
                if (chunk.size() == 1) {
                    // 1 sola localización: Open-Meteo devuelve un objeto, no array.
                    OpenMeteoResponse r = doFetch(chunk.get(0)[0], chunk.get(0)[1]);
                    putCache(chunk.get(0)[0], chunk.get(0)[1], r);
                } else {
                    OpenMeteoResponse[] arr = doFetchBatch(chunk);
                    for (int i = 0; i < chunk.size() && i < arr.length; i++) {
                        if (arr[i] != null) putCache(chunk.get(i)[0], chunk.get(i)[1], arr[i]);
                    }
                }
            } catch (RateLimitedException e) {
                cooldownUntil = System.currentTimeMillis() + Duration.ofMinutes(10).toMillis();
                log.warn("Open-Meteo 429 en prewarm: cooldown 10 min");
                return;   // no sigue martilleando
            } catch (Exception e) {
                log.warn("prewarm chunk falló ({} coords): {}", chunk.size(), e.toString());
                // sigue con el resto de chunks
            }
        }
    }

    private OpenMeteoResponse[] doFetchBatch(List<double[]> chunk) {
        String lats = chunk.stream().map(c -> String.valueOf(c[0])).collect(Collectors.joining(","));
        String lons = chunk.stream().map(c -> String.valueOf(c[1])).collect(Collectors.joining(","));
        return restClient.get()
                .uri(b -> b
                        .queryParam("latitude", lats)
                        .queryParam("longitude", lons)
                        .queryParam("hourly", HOURLY_VARS)
                        .queryParam("wind_speed_unit", "kmh")
                        .queryParam("timezone", "auto")
                        .queryParam("forecast_days", 7)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, resp) -> {
                    if (resp.getStatusCode().value() == 429) throw new RateLimitedException();
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "Open-Meteo batch HTTP " + resp.getStatusCode().value());
                })
                .body(OpenMeteoResponse[].class);
    }

    /** Clave de caché idéntica a la de @Cacheable("forecast") (#lat + ',' + #lon). */
    private static String cacheKey(double lat, double lon) { return lat + "," + lon; }

    private OpenMeteoResponse getCached(double lat, double lon) {
        Cache cache = cacheManager.getCache("forecast");
        if (cache == null) return null;
        Cache.ValueWrapper w = cache.get(cacheKey(lat, lon));
        return w == null ? null : (OpenMeteoResponse) w.get();
    }

    private void putCache(double lat, double lon, OpenMeteoResponse r) {
        Cache cache = cacheManager.getCache("forecast");
        if (cache != null && r != null) cache.put(cacheKey(lat, lon), r);
    }

    private static class RateLimitedException extends RuntimeException {}

    private OpenMeteoResponse doFetch(double lat, double lon) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("latitude", lat)
                            .queryParam("longitude", lon)
                            .queryParam("hourly", HOURLY_VARS)
                            .queryParam("wind_speed_unit", "kmh")
                            // timezone=auto → Open-Meteo devuelve las horas en la
                            // zona horaria del propio sitio (Madrid = CEST en
                            // verano) en vez de UTC. Sin esto, el array salía en
                            // GMT y el grid mostraba las horas con 1-2h de desfase
                            // respecto a la hora real. También devuelve
                            // utc_offset_seconds para localizar "ahora".
                            .queryParam("timezone", "auto")
                            .queryParam("forecast_days", 7)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        log.warn("Open-Meteo error: HTTP {} for lat={} lon={}", resp.getStatusCode(), lat, lon);
                        if (resp.getStatusCode().value() == 429) throw new RateLimitedException();
                        throw new ResponseStatusException(
                                HttpStatus.SERVICE_UNAVAILABLE,
                                "El servicio meteorológico no está disponible temporalmente (upstream HTTP "
                                        + resp.getStatusCode().value() + "). Inténtalo de nuevo en unos minutos."
                        );
                    })
                    .body(OpenMeteoResponse.class);
        } catch (RateLimitedException | ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Open-Meteo client error: {}", e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo obtener el forecast meteorológico (" + e.getClass().getSimpleName()
                            + ": " + e.getMessage() + ")."
            );
        }
    }
}
