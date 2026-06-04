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
                    + "precipitation_probability,wind_speed_10m,cloud_cover,dew_point_2m";

    private final RestClient restClient;

    public OpenMeteoClient() {
        // Timeouts cortos para que el back no se cuelgue cuando Open-Meteo está down
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(4).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(6).toMillis());
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory(factory)
                .build();
    }

    @Cacheable(value = "forecast", key = "#lat + ',' + #lon")
    public OpenMeteoResponse fetchForecast(double lat, double lon) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("latitude", lat)
                            .queryParam("longitude", lon)
                            .queryParam("hourly", HOURLY_VARS)
                            .queryParam("wind_speed_unit", "kmh")
                            .queryParam("forecast_days", 7)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        log.warn("Open-Meteo error: HTTP {} for lat={} lon={}", resp.getStatusCode(), lat, lon);
                        throw new ResponseStatusException(
                                HttpStatus.SERVICE_UNAVAILABLE,
                                "El servicio meteorológico no está disponible temporalmente. Inténtalo de nuevo en unos minutos."
                        );
                    })
                    .body(OpenMeteoResponse.class);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Open-Meteo client error: {}", e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo obtener el forecast meteorológico."
            );
        }
    }
}
