package com.meteomontana.api.infrastructure.weather;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.cache.annotation.Cacheable;
/**
 * Cliente HTTP para llamar a la API de Open-Meteo.
 * Encapsula la URL, los parámetros y la deserialización del JSON.
 */

@Component
public class OpenMeteoClient {

    private static final String BASE_URL = "https://api.open-meteo.com/v1/forecast";
    private static final String HOURLY_VARS =
            "temperature_2m,relative_humidity_2m,precipitation,"
                    + "precipitation_probability,wind_speed_10m,cloud_cover,dew_point_2m";

    private final RestClient restClient;

    public OpenMeteoClient(){
        //RestClient es el cliente HTTP moderno de Spring(sustituye a RestTemplate).
        //.baseUrl() fija la URL común - luego solo añadimos los query y params.
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .build();
    }

    //llama a Open-Meteo y devuelve la respuesta deserializada
    @Cacheable(value = "forecast", key = "#lat + ',' + #lon")
    public OpenMeteoResponse fetchForecast(double lat, double lon){
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("latitude", lat)
                        .queryParam("longitude", lon)
                        .queryParam("hourly", HOURLY_VARS)
                        .queryParam("wind_speed_unit" , "kmh")
                        .queryParam("forecast_days", 7)
                        .build())
                .retrieve()
                .body(OpenMeteoResponse.class);
    }



}
