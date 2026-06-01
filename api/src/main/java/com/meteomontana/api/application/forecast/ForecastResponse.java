package com.meteomontana.api.application.forecast;
import java.util.List;

/**
 * Respuesta que el endpoint /api/schools/{id}/forecast devolverá al cliente.
 *
 * No es el JSON crudo de Open-Meteo: lleva info de la escuela + el tiempo
 * hora por hora + su score precalculado.
 */
public record ForecastResponse (
        String schoolId,
        String schoolName,
        double lat,
        double lon,
        List<HourForecast> hours
){
    /**
     * Datos de una hora concreta: tiempo + score listo para mostrar.
     */
    public record HourForecast(
      String time,
      double temperature,
      double humidity,
      double windSpeed,
      double precipitation,
      int precipitationProbability,
      int cloudCover,
      double dewPoint,
      int score,   //calculado con ClimbScoreCalculator
      String scoreLabel //"excelente, bueno etc"
    ){}
}
