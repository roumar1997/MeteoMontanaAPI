package com.meteomontana.api.application.forecast;

import java.util.List;

/**
 * Respuesta del endpoint /api/schools/{id}/forecast.
 * Lleva info de la escuela + horas con score + resumen útil:
 *  - current con factores explicados (¿por qué este índice?)
 *  - ventana óptima del día
 *  - mejor día de los próximos 7
 *  - precipitación 24h / 72h
 */
public record ForecastResponse(
        String schoolId,
        String schoolName,
        double lat,
        double lon,
        Current current,
        List<HourForecast> hours,
        List<DayForecast> days,
        BestDay bestDay,
        OptimalWindow bestWindow
) {
    /** Hora actual con desglose de factores. */
    public record Current(
            String time,
            double temperature,
            double humidity,
            double windSpeed,
            double precipitation,
            int precipitationProbability,
            int cloudCover,
            Double dewPoint,
            double precip24h,
            double precip72h,
            boolean dryRock,
            int score,
            String scoreLabel,
            List<ScoreFactor> factors
    ) {}

    public record HourForecast(
            String time,
            double temperature,
            double humidity,
            double windSpeed,
            double precipitation,
            int precipitationProbability,
            int cloudCover,
            Double dewPoint,
            int score,
            String scoreLabel,
            int weatherCode
    ) {}

    public record DayForecast(
            String date,
            double tempMax,
            double tempMin,
            double precipitationTotal,
            int avgScore,
            String scoreLabel
    ) {}

    /** Mejor día de los próximos 7. */
    public record BestDay(String date, int score, String label, int daysFromToday) {}

    /** Ventana óptima del día actual (mejor rango de horas con score alto). */
    public record OptimalWindow(String start, String end, int avgScore) {}

    /** Ej: name="TEMPERATURA", display="29°", passes=false */
    public record ScoreFactor(String name, String display, boolean passes) {}
}
