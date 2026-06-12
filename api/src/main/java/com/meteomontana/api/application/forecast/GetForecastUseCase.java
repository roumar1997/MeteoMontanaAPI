package com.meteomontana.api.application.forecast;

import com.meteomontana.api.domain.exception.SchoolNotFoundException;
import com.meteomontana.api.domain.model.School;
import com.meteomontana.api.domain.port.SchoolRepository;
import com.meteomontana.api.domain.score.ClimbScoreCalculator;
import com.meteomontana.api.domain.score.RockDryingProfile;
import com.meteomontana.api.infrastructure.weather.OpenMeteoClient;
import com.meteomontana.api.infrastructure.weather.OpenMeteoResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GetForecastUseCase {

    private final SchoolRepository schoolRepository;
    private final OpenMeteoClient openMeteoClient;

    public GetForecastUseCase(SchoolRepository schoolRepository,
                              OpenMeteoClient openMeteoClient) {
        this.schoolRepository = schoolRepository;
        this.openMeteoClient = openMeteoClient;
    }

    /** Debe coincidir con el queryParam forecast_days de OpenMeteoClient. */
    private static final int FORECAST_DAYS = 7;

    public ForecastResponse execute(String schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new SchoolNotFoundException(schoolId));

        OpenMeteoResponse weather = openMeteoClient.fetchForecast(school.getLat(), school.getLon());

        // Open-Meteo nos da past_days=3 + forecast_days=7: las primeras 72
        // entradas son lluvia pasada REAL (para dryRock/hoursToDry y para que
        // recentRain de las primeras horas no se quede a cero). La respuesta
        // al cliente mantiene la forma de siempre: horas desde las 00:00 de hoy.
        List<ForecastResponse.HourForecast> allHours = buildHourlyForecast(weather, school.getRockType());
        int todayStart = Math.max(0, allHours.size() - FORECAST_DAYS * 24);
        List<ForecastResponse.HourForecast> hours = List.copyOf(allHours.subList(todayStart, allHours.size()));

        List<ForecastResponse.DayForecast>  days  = buildDailyForecast(weather, hours);
        int nowIdx = todayStart + indexOfCurrentHour(hours);
        ForecastResponse.Current current          = buildCurrent(weather, allHours, nowIdx, school.getRockType());
        ForecastResponse.BestDay bestDay          = pickBestDay(days);
        ForecastResponse.OptimalWindow window     = pickOptimalWindow(hours, current.time());

        return new ForecastResponse(
                school.getId(), school.getName(), school.getLat(), school.getLon(),
                current, hours, days, bestDay, window
        );
    }

    // ──────────────────────────────────────────────────────────────────────────
    // HOURLY
    // ──────────────────────────────────────────────────────────────────────────

    private List<ForecastResponse.HourForecast> buildHourlyForecast(
            OpenMeteoResponse weather, String rockType) {

        OpenMeteoResponse.HourlyData h = weather.hourly();
        int lookback = RockDryingProfile.forRockType(rockType).lookbackHours();
        List<ForecastResponse.HourForecast> result = new ArrayList<>(h.time().size());

        for (int i = 0; i < h.time().size(); i++) {
            double recentRain = 0;
            for (int j = Math.max(0, i - lookback); j < i; j++) {
                recentRain += h.precipitation().get(j);
            }
            double temp     = h.temperature().get(i);
            double humidity = h.humidity().get(i);
            double wind     = h.windSpeed().get(i);
            double precip   = h.precipitation().get(i);
            int    prob     = h.precipitationProbability().get(i);
            int    cloud       = h.cloudCover()   != null ? h.cloudCover().get(i)   : 50;
            Double dewPoint    = h.dewPoint()     != null ? h.dewPoint().get(i)     : null;
            int    weatherCode = h.weatherCode()  != null ? h.weatherCode().get(i)  : 0;

            int score = ClimbScoreCalculator.calculate(
                    temp, humidity, wind, precip, prob, cloud, recentRain, dewPoint, rockType);

            result.add(new ForecastResponse.HourForecast(
                    h.time().get(i),
                    temp, humidity, wind, precip, prob, cloud, dewPoint,
                    score, ClimbScoreCalculator.label(score), weatherCode
            ));
        }
        return result;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // DAILY (agrupando por fecha)
    // ──────────────────────────────────────────────────────────────────────────

    private List<ForecastResponse.DayForecast> buildDailyForecast(
            OpenMeteoResponse weather, List<ForecastResponse.HourForecast> hours) {

        // Agrupamos índices por día (yyyy-MM-dd).
        Map<String, List<Integer>> byDay = new LinkedHashMap<>();
        for (int i = 0; i < hours.size(); i++) {
            String date = hours.get(i).time().substring(0, 10);
            byDay.computeIfAbsent(date, k -> new ArrayList<>()).add(i);
        }

        List<ForecastResponse.DayForecast> days = new ArrayList<>();
        for (var entry : byDay.entrySet()) {
            String date = entry.getKey();
            List<Integer> idxs = entry.getValue();

            double tMax = -Double.MAX_VALUE, tMin = Double.MAX_VALUE, precipTotal = 0;
            int scoreSum = 0;
            for (int idx : idxs) {
                var hf = hours.get(idx);
                tMax = Math.max(tMax, hf.temperature());
                tMin = Math.min(tMin, hf.temperature());
                precipTotal += hf.precipitation();
                scoreSum += hf.score();
            }
            int avgScore = scoreSum / idxs.size();
            days.add(new ForecastResponse.DayForecast(
                    date, round1(tMax), round1(tMin),
                    round1(precipTotal), avgScore, ClimbScoreCalculator.label(avgScore)
            ));
        }
        return days;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CURRENT con factores explicados
    // ──────────────────────────────────────────────────────────────────────────

    private ForecastResponse.Current buildCurrent(
            OpenMeteoResponse weather,
            List<ForecastResponse.HourForecast> allHours,
            int nowIdx,
            String rockType) {

        var cur = allHours.get(nowIdx);
        OpenMeteoResponse.HourlyData h = weather.hourly();

        // Lluvia acumulada REAL hacia atrás: el array incluye past_days=3,
        // así que [nowIdx-72, nowIdx) son horas que ya han pasado.
        double precip24h = sumPrecipBack(h, nowIdx, 24);
        double precip72h = sumPrecipBack(h, nowIdx, 72);

        // Roca seca si lluvia 72h < cap por tipo de roca.
        // Perfil "seca rápido" (granito, capMult 1.30) tolera más lluvia;
        // "seca lento" (arenisca, capMult 0.45) tolera muy poca.
        double dryThreshold = 6.0 * RockDryingProfile.forRockType(rockType).capMult(); // mm
        boolean dryRock = precip72h < dryThreshold;
        Integer hoursToDry = computeHoursToDry(h.precipitation(), nowIdx, dryThreshold);

        List<ForecastResponse.ScoreFactor> factors = buildFactors(cur, precip24h, precip72h);

        return new ForecastResponse.Current(
                cur.time(), cur.temperature(), cur.humidity(), cur.windSpeed(),
                cur.precipitation(), cur.precipitationProbability(), cur.cloudCover(),
                cur.dewPoint(), round1(precip24h), round1(precip72h),
                dryRock, hoursToDry, cur.score(), cur.scoreLabel(), factors
        );
    }

    /** Última hora del array cuyo timestamp ya ha pasado (las horas vienen en GMT). */
    private static int indexOfCurrentHour(List<ForecastResponse.HourForecast> hours) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        int idx = 0;
        for (int i = 0; i < hours.size(); i++) {
            if (LocalDateTime.parse(hours.get(i).time()).isAfter(now)) break;
            idx = i;
        }
        return idx;
    }

    /** Suma la precipitación de las `len` horas anteriores a `end` (exclusivo). */
    private double sumPrecipBack(OpenMeteoResponse.HourlyData h, int end, int len) {
        double sum = 0;
        for (int i = Math.max(0, end - len); i < Math.min(end, h.precipitation().size()); i++) {
            sum += h.precipitation().get(i);
        }
        return sum;
    }

    /**
     * Horas que faltan para que la roca se considere seca, con la MISMA métrica
     * que dryRock: acumulado de las 72h anteriores < umbral del tipo de roca.
     * Avanza hora a hora por el forecast — la roca "se seca" cuando la lluvia
     * pasada va saliendo de la ventana de 72h sin que entre lluvia nueva.
     * 0 = seca ya; null = sigue mojada al final del horizonte de 7 días.
     */
    static Integer computeHoursToDry(List<Double> precip, int nowIdx, double threshold) {
        for (int t = nowIdx; t < precip.size(); t++) {
            double sum = 0;
            for (int j = Math.max(0, t - 72); j < t; j++) sum += precip.get(j);
            if (sum < threshold) return t - nowIdx;
        }
        return null;
    }

    /**
     * Factores que pinta la PWA en el acordeón "¿Por qué este índice?".
     * Cada factor tiene un threshold simple — si lo cumple, ✓; si no, ❌.
     */
    private List<ForecastResponse.ScoreFactor> buildFactors(
            ForecastResponse.HourForecast cur, double precip24h, double precip72h) {

        List<ForecastResponse.ScoreFactor> list = new ArrayList<>();

        // Temperatura óptima: entre 5 y 22°C. Por encima penaliza.
        list.add(new ForecastResponse.ScoreFactor(
                "TEMPERATURA", Math.round(cur.temperature()) + "°",
                cur.temperature() >= 5 && cur.temperature() <= 22
        ));
        // Humedad: <70% bien.
        list.add(new ForecastResponse.ScoreFactor(
                "HUMEDAD", Math.round(cur.humidity()) + "%",
                cur.humidity() < 70
        ));
        // Viento: <25 km/h bien.
        list.add(new ForecastResponse.ScoreFactor(
                "VIENTO", Math.round(cur.windSpeed()) + " km/h",
                cur.windSpeed() < 25
        ));
        list.add(new ForecastResponse.ScoreFactor(
                "LLUVIA 24H", round1(precip24h) + " mm",
                precip24h < 1.0
        ));
        list.add(new ForecastResponse.ScoreFactor(
                "LLUVIA 72H", round1(precip72h) + " mm",
                precip72h < 6.0
        ));
        boolean dryRock = precip72h < 1.0;
        list.add(new ForecastResponse.ScoreFactor(
                "SEQUEDAD AIRE", dryRock ? "Roca seca" : "Roca húmeda",
                dryRock
        ));
        return list;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // BEST DAY (próximos 7)
    // ──────────────────────────────────────────────────────────────────────────

    private ForecastResponse.BestDay pickBestDay(List<ForecastResponse.DayForecast> days) {
        if (days.isEmpty()) return null;
        int bestIdx = 0;
        for (int i = 1; i < days.size(); i++) {
            if (days.get(i).avgScore() > days.get(bestIdx).avgScore()) bestIdx = i;
        }
        var d = days.get(bestIdx);
        return new ForecastResponse.BestDay(d.date(), d.avgScore(), d.scoreLabel(), bestIdx);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // OPTIMAL WINDOW del día actual
    // ──────────────────────────────────────────────────────────────────────────

    private ForecastResponse.OptimalWindow pickOptimalWindow(
            List<ForecastResponse.HourForecast> hours, String currentTime) {
        return bestWindowForDate(hours, currentTime.substring(0, 10));
    }

    /**
     * Mejor ventana de 4h consecutivas (por score promedio) de la fecha dada
     * (yyyy-MM-dd). Estático y público porque la alerta de tiempo lo reusa
     * para anunciar la mejor franja del día ganador.
     */
    public static ForecastResponse.OptimalWindow bestWindowForDate(
            List<ForecastResponse.HourForecast> hours, String date) {

        int windowSize = 4;
        int bestStart = -1;
        double bestAvg = -1;

        List<Integer> dayIdx = new ArrayList<>();
        for (int i = 0; i < hours.size(); i++) {
            if (hours.get(i).time().startsWith(date)) dayIdx.add(i);
        }
        if (dayIdx.size() < windowSize) return null;

        for (int s = 0; s + windowSize <= dayIdx.size(); s++) {
            double sum = 0;
            for (int k = 0; k < windowSize; k++) sum += hours.get(dayIdx.get(s + k)).score();
            double avg = sum / windowSize;
            if (avg > bestAvg) { bestAvg = avg; bestStart = s; }
        }
        if (bestStart < 0) return null;

        String startTime = hours.get(dayIdx.get(bestStart)).time().substring(11, 16);
        String endTime   = hours.get(dayIdx.get(bestStart + windowSize - 1)).time().substring(11, 16);
        return new ForecastResponse.OptimalWindow(startTime, endTime, (int) bestAvg);
    }

    // ──────────────────────────────────────────────────────────────────────────

    private static double round1(double v) { return Math.round(v * 10) / 10.0; }
}
