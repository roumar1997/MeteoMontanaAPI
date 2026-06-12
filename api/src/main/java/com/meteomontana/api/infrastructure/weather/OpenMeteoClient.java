package com.meteomontana.api.infrastructure.weather;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Duration;

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

    private final RestClient restClient;

    public OpenMeteoClient() {
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

    private static class RateLimitedException extends RuntimeException {}

    private OpenMeteoResponse doFetch(double lat, double lon) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("latitude", lat)
                            .queryParam("longitude", lon)
                            .queryParam("hourly", HOURLY_VARS)
                            .queryParam("wind_speed_unit", "kmh")
                            // 72h de lluvia pasada real para dryRock/hoursToDry
                            // (antes se aproximaba con horas futuras).
                            .queryParam("past_days", 3)
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
