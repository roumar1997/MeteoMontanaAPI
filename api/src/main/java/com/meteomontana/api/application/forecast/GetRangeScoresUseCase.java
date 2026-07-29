package com.meteomontana.api.application.forecast;

import com.meteomontana.api.domain.port.SchoolRepository;
import com.meteomontana.api.infrastructure.weather.OpenMeteoClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

/**
 * Score de un TRAMO de varios días (los que el usuario elige) para cada escuela.
 * Pensado para el selector de días de la lista de escuelas.
 *
 * IMPORTANTE sobre la lluvia: NO aplicamos una penalización propia por mm. El
 * score diario ({@code avgScore}) YA modela la lluvia y el SECADO por tipo de
 * roca: cada hora se calcula con la lluvia reciente de una ventana que depende
 * de la piedra ({@link RockDryingProfile}: arenisca 72h, granito 12h…), así que
 * un día posterior a la lluvia ya sale con score bajo en arenisca y se recupera
 * antes en granito. Por eso el combinado es simplemente la media de los días
 * elegidos; añadir un castigo por mm sería redundante e ignoraría la roca.
 * Los campos de lluvia (rainy/rainDays/maxRainMm) son solo informativos para
 * marcar en la UI qué días llueve.
 *
 * Reutiliza {@link GetForecastUseCase} (cacheado) — un forecast por escuela.
 * Cacheado por (ids, dates): como las fechas rotan cada día, la clave se
 * renueva sola.
 */
@Service
@RequiredArgsConstructor
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
            int combinedScore,        // media de los días (el score diario ya modela el secado por roca)
            int avgScore,             // == combinedScore (se mantiene por compatibilidad del DTO)
            List<DayScoreDto> days,
            int rainDays,
            double maxRainMm
    ) {}

    private final GetForecastUseCase forecastUseCase;
    private final SchoolRepository schoolRepository;
    private final OpenMeteoClient openMeteoClient;

    /**
     * @param ids   escuelas a evaluar (máx 60 por call).
     * @param dates fechas ISO (yyyy-MM-dd) elegidas por el usuario (máx 5).
     */
    @Cacheable(value = "range-scores", key = "#ids.toString() + '|' + #dates.toString()")
    public List<RangeScoreDto> forIds(List<String> ids, List<String> dates) {
        if (ids == null || ids.isEmpty() || dates == null || dates.isEmpty()) return List.of();
        List<String> limited = ids.stream().limit(MAX_IDS).toList();
        // Pre-calienta la caché con pocas llamadas batch (evita el pico de 429).
        openMeteoClient.prewarm(limited.stream()
                .map(id -> schoolRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .map(s -> new double[]{s.getLat(), s.getLon()})
                .toList());
        List<RangeScoreDto> out = new ArrayList<>();
        for (String id : limited) {
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
        int sumScore = 0, rainDays = 0;
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
            if (rainy) { rainDays++; maxRainMm = Math.max(maxRainMm, mm); }

            days.add(new DayScoreDto(date, d.avgScore(), round1(mm), prob, rainy));
        }

        if (days.isEmpty()) return new RangeScoreDto(id, 0, 0, List.of(), 0, 0);
        // Media de los días. El score diario ya penaliza la lluvia y el secado
        // por roca (ventana de lluvia reciente por tipo de piedra) → no añadimos
        // castigo propio (sería redundante e ignoraría la roca).
        int avg = Math.round((float) sumScore / days.size());
        return new RangeScoreDto(id, avg, avg, days, rainDays, round1(maxRainMm));
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
