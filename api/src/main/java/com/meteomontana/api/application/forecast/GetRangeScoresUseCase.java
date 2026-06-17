package com.meteomontana.api.application.forecast;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Score de un TRAMO de varios días (los que el usuario elige) para cada escuela.
 * Pensado para el selector de días de la lista de escuelas: combina el score
 * diario con una penalización por lluvia (mm/%) y dice en qué días llueve.
 *
 * Reutiliza {@link GetForecastUseCase} (cacheado) — un forecast por escuela.
 * Cacheado por (ids, dates): como las fechas rotan cada día, la clave se
 * renueva sola.
 */
@Service
public class GetRangeScoresUseCase {

    /** Un día cuenta como "con lluvia" a partir de este acumulado (mismo umbral que la alerta de finde). */
    private static final double RAIN_DAY_MM = 1.0;
    /** …o si la probabilidad máxima del día es alta aunque la cantidad estimada sea baja. */
    private static final int RAIN_PROB = 60;
    /** Máximo de escuelas por llamada (la app encadena lotes). */
    private static final int MAX_IDS = 60;

    /** Resumen de un día dentro del tramo. */
    public record DayScoreDto(
            String date,
            int score,
            double rainMm,
            int rainProb,
            boolean rainy
    ) {}

    /** Resumen del tramo para una escuela. */
    public record RangeScoreDto(
            String id,
            int combinedScore,        // media de días − penalización por lluvia
            int avgScore,             // media simple (sin penalizar), informativo
            List<DayScoreDto> days,
            int rainDays,
            double maxRainMm
    ) {}

    private final GetForecastUseCase forecastUseCase;

    public GetRangeScoresUseCase(GetForecastUseCase forecastUseCase) {
        this.forecastUseCase = forecastUseCase;
    }

    /**
     * @param ids   escuelas a evaluar (máx 60 por call).
     * @param dates fechas ISO (yyyy-MM-dd) elegidas por el usuario (máx 5).
     */
    @Cacheable(value = "range-scores", key = "#ids.toString() + '|' + #dates.toString()")
    public List<RangeScoreDto> forIds(List<String> ids, List<String> dates) {
        if (ids == null || ids.isEmpty() || dates == null || dates.isEmpty()) return List.of();
        List<RangeScoreDto> out = new ArrayList<>();
        for (String id : ids.stream().limit(MAX_IDS).toList()) {
            try {
                ForecastResponse fc = forecastUseCase.execute(id);
                out.add(summarize(id, fc, dates));
            } catch (Exception e) {
                out.add(new RangeScoreDto(id, 0, 0, List.of(), 0, 0));
            }
        }
        return out;
    }

    private RangeScoreDto summarize(String id, ForecastResponse fc, List<String> dates) {
        List<DayScoreDto> days = new ArrayList<>();
        int sumScore = 0, penalty = 0, rainDays = 0;
        double maxRainMm = 0;

        for (String date : dates) {
            ForecastResponse.DayForecast d = fc.days().stream()
                    .filter(x -> x.date().equals(date))
                    .findFirst().orElse(null);
            if (d == null) continue;

            int prob = maxRainProbFor(fc, date);
            double mm = d.precipitationTotal();
            boolean rainy = mm >= RAIN_DAY_MM || prob >= RAIN_PROB;

            sumScore += d.avgScore();
            penalty += rainPenalty(mm, prob);
            if (rainy) { rainDays++; maxRainMm = Math.max(maxRainMm, mm); }

            days.add(new DayScoreDto(date, d.avgScore(), round1(mm), prob, rainy));
        }

        if (days.isEmpty()) return new RangeScoreDto(id, 0, 0, List.of(), 0, 0);
        int avg = Math.round((float) sumScore / days.size());
        int combined = Math.max(0, avg - penalty);
        return new RangeScoreDto(id, combined, avg, days, rainDays, round1(maxRainMm));
    }

    /** Penalización por la lluvia de UN día: escalada por mm y, en su defecto, por % de probabilidad. */
    private static int rainPenalty(double mm, int prob) {
        if (mm >= 8) return 25;
        if (mm >= 3) return 15;
        if (mm >= 1) return 8;
        if (prob >= RAIN_PROB) return 6;   // poca cantidad pero alto riesgo
        return 0;
    }

    /** Probabilidad de precipitación máxima de las horas de ese día. */
    private static int maxRainProbFor(ForecastResponse fc, String date) {
        return fc.hours().stream()
                .filter(h -> h.time() != null && h.time().startsWith(date))
                .mapToInt(ForecastResponse.HourForecast::precipitationProbability)
                .max().orElse(0);
    }

    private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }
}
