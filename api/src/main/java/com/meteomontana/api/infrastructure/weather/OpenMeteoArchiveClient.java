package com.meteomontana.api.infrastructure.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * Cliente del archivo histórico de Open-Meteo (archive-api.open-meteo.com).
 * Descarga 3 años de datos diarios para calcular los scores mensuales de
 * una escuela. Antes esta llamada la hacía cada móvil Android directamente;
 * ahora se hace una vez aquí y se cachea (GetMonthlyStatsUseCase).
 */
@Component
public class OpenMeteoArchiveClient {

    private static final Logger log = LoggerFactory.getLogger(OpenMeteoArchiveClient.class);

    private static final String BASE_URL = "https://archive-api.open-meteo.com/v1/archive";
    private static final String DAILY_VARS =
            "temperature_2m_max,precipitation_sum,wind_speed_10m_max,relative_humidity_2m_mean";

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ArchiveResponse(Daily daily) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Daily(
            List<String> time,
            List<Double> temperature_2m_max,
            List<Double> precipitation_sum,
            List<Double> wind_speed_10m_max,
            List<Double> relative_humidity_2m_mean
    ) {}

    private final RestClient restClient;

    public OpenMeteoArchiveClient() {
        // La respuesta son ~3 años de datos diarios; algo más de margen que el forecast.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(15).toMillis());
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory(factory)
                .build();
    }

    public ArchiveResponse fetchDailyHistory(double lat, double lon) {
        int endYear = LocalDate.now().getYear() - 1;
        int startYear = endYear - 2;
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("latitude", lat)
                            .queryParam("longitude", lon)
                            .queryParam("start_date", startYear + "-01-01")
                            .queryParam("end_date", endYear + "-12-31")
                            .queryParam("daily", DAILY_VARS)
                            .queryParam("timezone", "auto")
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        log.warn("Open-Meteo archive error: HTTP {} for lat={} lon={}",
                                resp.getStatusCode(), lat, lon);
                        throw new ResponseStatusException(
                                HttpStatus.SERVICE_UNAVAILABLE,
                                "El histórico meteorológico no está disponible temporalmente."
                        );
                    })
                    .body(ArchiveResponse.class);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Open-Meteo archive client error: {}", e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo obtener el histórico meteorológico."
            );
        }
    }
}
