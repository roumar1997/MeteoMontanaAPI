package com.meteomontana.api.infrastructure.weather;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTO que mapea la respuesta de Open-Meteo.
 * Jackson (la librería JSON de Spring) deserializa el JSON
 * automáticamente en estos campos.
 */
public record OpenMeteoResponse (
        double latitude,
        double longitude,
        double elevation,

        // Offset de la zona horaria del sitio respecto a UTC, en segundos
        // (lo manda Open-Meteo al pedir timezone=auto). Sirve para saber qué
        // hora local es "ahora" y localizar el índice presente del array.
        @JsonProperty("utc_offset_seconds")
        int utcOffsetSeconds,

        HourlyData hourly
) {
    public record HourlyData(
            List<String> time,

            @JsonProperty("temperature_2m")
            List<Double> temperature,

            @JsonProperty("relative_humidity_2m")
            List<Double> humidity,

            List<Double> precipitation,

            @JsonProperty("precipitation_probability")
            List<Integer> precipitationProbability,

            @JsonProperty("wind_speed_10m")
            List<Double> windSpeed,

            @JsonProperty("cloud_cover")
            List<Integer> cloudCover,

            @JsonProperty("dew_point_2m")
            List<Double> dewPoint,

            @JsonProperty("weather_code")
            List<Integer> weatherCode,

            /** W/m². Null en cachés antiguas (campo añadido 2026-07-28) —
             *  RockTemperatureModel lo tolera (sin sol = roca sigue al aire). */
            @JsonProperty("shortwave_radiation")
            List<Double> radiation
    ){}
}
