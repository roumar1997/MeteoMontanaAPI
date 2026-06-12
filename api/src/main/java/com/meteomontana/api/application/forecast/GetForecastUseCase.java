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

    public ForecastResponse execute(String schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new SchoolNotFoundException(schoolId));

        OpenMeteoResponse weather = openMeteoClient.fetchForecast(school.getLat(), school.getLon());

        List<ForecastResponse.HourForecast> hours = buildHourlyForecast(weather, school.getRockType());
        List<ForecastResponse.DayForecast>  days  = buildDailyForecast(weather, hours);
        ForecastResponse.Current current          = buildCurrent(weather, hours, school.getRockType());
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
            List<ForecastResponse.HourForecast> hours,
            String rockType) {

        // Índice 0 = "ahora" (Open-Meteo devuelve desde la hora actual redondeada).
        var cur = hours.get(0);
        OpenMeteoResponse.HourlyData h = weather.hourly();

        // Lluvia acumulada 24h y 72h hacia atrás. Open-Meteo solo da forecast,
        // así que aproximamos con las primeras N horas que ya pasaron del día
        // (no es exacto, pero útil para el UI). En realidad la app debería
        // tener un endpoint con histórico — TODO.
        // De momento: usamos las próximas 24/72 como proxy (es mejor que nada).
        double precip24h = sumPrecip(h, 0, 24);
        double precip72h = sumPrecip(h, 0, 72);

        boolean dryRock = isDryRock(cur, precip72h, rockType);

        List<ForecastResponse.ScoreFactor> factors = buildFactors(cur, precip24h, precip72h);
        ForecastResponse.RockDrying drying = buildDrying(rockType, precip72h);

        return new ForecastResponse.Current(
                cur.time(), cur.temperature(), cur.humidity(), cur.windSpeed(),
                cur.precipitation(), cur.precipitationProbability(), cur.cloudCover(),
                cur.dewPoint(), round1(precip24h), round1(precip72h),
                dryRock, cur.score(), cur.scoreLabel(), factors, drying
        );
    }

    /**
     * Tiempo de secado estimado tras lluvia, por tipo de roca. Usa el mismo
     * proxy de lluvia 72h que isDryRock (no tenemos histórico real — TODO arriba)
     * y deriva las horas del lookback del perfil: ~2/3 del lookback da los
     * valores acordados (caliza 18h→12h, arenisca 72h→48h, granito 12h→8h).
     * Con lluvia fuerte (≥2× el umbral del perfil) se alarga un 50%.
     */
    private ForecastResponse.RockDrying buildDrying(String rockType, double precip72h) {
        RockDryingProfile profile = RockDryingProfile.forRockType(rockType);
        double threshold = 6.0 * profile.capMult(); // mismo umbral que isDryRock
        int baseHours = (int) Math.round(profile.lookbackHours() * 2.0 / 3.0);
        boolean sandstone = rockType != null && rockType.toLowerCase().contains("arenisca");

        boolean wet = precip72h >= threshold;
        if (!wet) {
            // La arenisca es frágil mojada por dentro: avisamos aunque "parezca" seca
            // si ha caído algo de lluvia en la ventana.
            if (sandstone && precip72h >= 1.0) {
                return new ForecastResponse.RockDrying(false, baseHours,
                        "Arenisca: evita escalar " + baseHours + " h tras lluvia");
            }
            return new ForecastResponse.RockDrying(false, null, null);
        }

        int hours = precip72h >= threshold * 2
                ? (int) Math.round(baseHours * 1.5)
                : baseHours;
        String message = sandstone
                ? "Arenisca: no escalar hasta ~" + hours + " h tras la lluvia"
                : "Seca en ~" + hours + " h";
        return new ForecastResponse.RockDrying(true, hours, message);
    }

    private double sumPrecip(OpenMeteoResponse.HourlyData h, int from, int len) {
        double sum = 0;
        for (int i = from; i < Math.min(from + len, h.precipitation().size()); i++) {
            sum += h.precipitation().get(i);
        }
        return sum;
    }

    /** Heurística simple: roca seca si lluvia 72h < cap por tipo de roca. */
    private boolean isDryRock(ForecastResponse.HourForecast cur, double precip72h, String rockType) {
        double mult = RockDryingProfile.forRockType(rockType).capMult();
        // Si el perfil "seca rápido" (granito, capMult 1.30), tolera más lluvia.
        // Si "seca lento" (arenisca, capMult 0.45), tolera muy poca.
        double threshold = 6.0 * mult; // mm — ajustable
        return precip72h < threshold;
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

        String today = currentTime.substring(0, 10);
        // Buscamos la ventana de 4h consecutivas con mejor score promedio del día actual.
        int windowSize = 4;
        int bestStart = -1;
        double bestAvg = -1;

        List<Integer> todayIdx = new ArrayList<>();
        for (int i = 0; i < hours.size(); i++) {
            if (hours.get(i).time().startsWith(today)) todayIdx.add(i);
        }
        if (todayIdx.size() < windowSize) return null;

        for (int s = 0; s + windowSize <= todayIdx.size(); s++) {
            double sum = 0;
            for (int k = 0; k < windowSize; k++) sum += hours.get(todayIdx.get(s + k)).score();
            double avg = sum / windowSize;
            if (avg > bestAvg) { bestAvg = avg; bestStart = s; }
        }
        if (bestStart < 0) return null;

        String startTime = hours.get(todayIdx.get(bestStart)).time().substring(11, 16);
        String endTime   = hours.get(todayIdx.get(bestStart + windowSize - 1)).time().substring(11, 16);
        return new ForecastResponse.OptimalWindow(startTime, endTime, (int) bestAvg);
    }

    // ──────────────────────────────────────────────────────────────────────────

    private static double round1(double v) { return Math.round(v * 10) / 10.0; }
}
