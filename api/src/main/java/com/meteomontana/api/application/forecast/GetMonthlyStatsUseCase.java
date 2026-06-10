package com.meteomontana.api.application.forecast;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.meteomontana.api.domain.exception.SchoolNotFoundException;
import com.meteomontana.api.domain.model.School;
import com.meteomontana.api.domain.port.SchoolRepository;
import com.meteomontana.api.infrastructure.weather.OpenMeteoArchiveClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Scores mensuales (0-100, uno por mes) de una escuela a partir de 3 años de
 * histórico diario de Open-Meteo, más el rango de mejores meses.
 *
 * Port del cálculo que hacía la app Android en ClimbScore.kt/OpenMeteoArchive.kt
 * — mantener ambos en sincronía si se ajusta la fórmula.
 *
 * Cache propia de Caffeine con TTL 30 días: el histórico de años pasados no
 * cambia, y el TTL global de 30 min de spring.cache sería absurdo aquí.
 */
@Service
public class GetMonthlyStatsUseCase {

    public record MonthlyStatsResponse(List<Integer> scores, String bestRange) {}

    private static final String[] MONTH_NAMES = {
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    };

    private final SchoolRepository schoolRepository;
    private final OpenMeteoArchiveClient archiveClient;

    private final Cache<String, MonthlyStatsResponse> cache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofDays(30))
            .build();

    public GetMonthlyStatsUseCase(SchoolRepository schoolRepository,
                                  OpenMeteoArchiveClient archiveClient) {
        this.schoolRepository = schoolRepository;
        this.archiveClient = archiveClient;
    }

    public MonthlyStatsResponse execute(String schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new SchoolNotFoundException(schoolId));
        return cache.get(schoolId, id -> compute(school));
    }

    private MonthlyStatsResponse compute(School school) {
        var resp = archiveClient.fetchDailyHistory(school.getLat(), school.getLon());
        var daily = resp != null ? resp.daily() : null;
        if (daily == null || daily.time() == null || daily.time().isEmpty()) {
            return new MonthlyStatsResponse(List.of(0,0,0,0,0,0,0,0,0,0,0,0), null);
        }

        List<List<Integer>> perMonth = new ArrayList<>(12);
        for (int m = 0; m < 12; m++) perMonth.add(new ArrayList<>());

        for (int i = 0; i < daily.time().size(); i++) {
            Double temp = at(daily.temperature_2m_max(), i);
            Double rain = at(daily.precipitation_sum(), i);
            Double wind = at(daily.wind_speed_10m_max(), i);
            Double hum  = at(daily.relative_humidity_2m_mean(), i);
            if (temp == null || rain == null || wind == null || hum == null) continue;
            int precipProb = rain > 0.5 ? 80 : 10;
            int month = Integer.parseInt(daily.time().get(i).substring(5, 7)) - 1;
            perMonth.get(month).add(
                    climbScoreDaily(temp, hum, wind, rain, precipProb, school.getRockType()));
        }

        List<Integer> avg = perMonth.stream()
                .map(scores -> scores.isEmpty() ? 0
                        : (int) scores.stream().mapToInt(Integer::intValue).average().orElse(0))
                .toList();
        return new MonthlyStatsResponse(avg, computeBestRange(avg));
    }

    private static Double at(List<Double> list, int i) {
        return (list != null && i < list.size()) ? list.get(i) : null;
    }

    /** Mismo score que ClimbScore.kt de la app: temp 30%, lluvia 30%, hum 20%, viento 20%. */
    static int climbScoreDaily(double tempMax, double humidity, double windKmh,
                               double precipMm, int precipProb, String rockType) {
        double tempScore;
        if (tempMax >= 12.0 && tempMax <= 22.0)      tempScore = 100.0;
        else if (tempMax >= 5.0 && tempMax <= 28.0)  tempScore = 70.0;
        else if (tempMax >= 0.0 && tempMax <= 32.0)  tempScore = 40.0;
        else                                          tempScore = 10.0;

        double humScore;
        if (humidity < 50)      humScore = 100.0;
        else if (humidity < 65) humScore = 80.0;
        else if (humidity < 80) humScore = 50.0;
        else                    humScore = 20.0;

        double windScore;
        if (windKmh >= 4.0 && windKmh <= 22.0)      windScore = 100.0;
        else if (windKmh >= 1.0 && windKmh <= 30.0) windScore = 70.0;
        else if (windKmh < 1.0)                      windScore = 50.0;
        else                                         windScore = 30.0;

        double rainCap = switch (rockType == null ? "" : rockType.toUpperCase()) {
            case "CALIZA"   -> 5.0;
            case "GRANITO"  -> 8.0;
            case "ARENISCA" -> 3.0;
            default         -> 6.0;
        };
        double rainScore;
        if (precipMm <= 0.1 && precipProb < 20) {
            rainScore = 100.0;
        } else {
            double base = 100.0 * (1.0 - Math.min(1.0, Math.max(0.0, precipMm / rainCap)));
            rainScore = Math.max(0.0, base - precipProb * 0.3);
        }

        double score = tempScore * 0.30 + rainScore * 0.30 + humScore * 0.20 + windScore * 0.20;
        return (int) Math.min(100.0, Math.max(0.0, score));
    }

    /** Ventana de 2-6 meses consecutivos con mejor media, ej. "Octubre-Mayo". */
    private static String computeBestRange(List<Integer> scores) {
        if (scores.stream().allMatch(s -> s == 0)) return null;
        double bestAvg = -1.0;
        int bestStart = -1, bestEnd = -1;
        for (int len = 2; len <= 6; len++) {
            for (int start = 0; start + len <= 12; start++) {
                double avg = scores.subList(start, start + len).stream()
                        .mapToInt(Integer::intValue).average().orElse(0);
                if (avg > bestAvg) {
                    bestAvg = avg;
                    bestStart = start;
                    bestEnd = start + len - 1;
                }
            }
        }
        if (bestStart < 0) return null;
        return MONTH_NAMES[bestStart] + "-" + MONTH_NAMES[bestEnd];
    }
}
