package com.meteomontana.api.application.forecast;

import com.meteomontana.api.domain.score.ClimbScoreCalculator;
import com.meteomontana.api.domain.score.RockDryingProfile;
import com.meteomontana.api.infrastructure.weather.OpenMeteoClient;
import com.meteomontana.api.infrastructure.weather.OpenMeteoResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Forecast genérico para una ubicación arbitraria.
 * Útil para el tab Tiempo: "el tiempo en mi ubicación".
 * No requiere escuela — usa caliza como roca por defecto.
 */
@Service
public class GetForecastByLocationUseCase {

    private final OpenMeteoClient openMeteoClient;

    public GetForecastByLocationUseCase(OpenMeteoClient openMeteoClient) {
        this.openMeteoClient = openMeteoClient;
    }

    public ForecastResponse execute(double lat, double lon, String rockType) {
        OpenMeteoResponse weather = openMeteoClient.fetchForecast(lat, lon);
        String rock = rockType != null ? rockType : "Caliza";

        // Reuso lógica del GetForecastUseCase manualmente porque
        // los métodos privados no son accesibles. Copiamos lo esencial.
        OpenMeteoResponse.HourlyData h = weather.hourly();
        int lookback = RockDryingProfile.forRockType(rock).lookbackHours();
        List<ForecastResponse.HourForecast> allHours = new ArrayList<>(h.time().size());

        for (int i = 0; i < h.time().size(); i++) {
            double recentRain = 0;
            for (int j = Math.max(0, i - lookback); j < i; j++) {
                recentRain += h.precipitation().get(j);
            }
            double temp = h.temperature().get(i);
            double humidity = h.humidity().get(i);
            double wind = h.windSpeed().get(i);
            double precip = h.precipitation().get(i);
            int prob = h.precipitationProbability().get(i);
            int    cloud       = h.cloudCover()  != null ? h.cloudCover().get(i)  : 50;
            Double dewPoint    = h.dewPoint()    != null ? h.dewPoint().get(i)    : null;
            int    weatherCode = h.weatherCode() != null ? h.weatherCode().get(i) : 0;

            int score = ClimbScoreCalculator.calculate(
                    temp, humidity, wind, precip, prob, cloud, recentRain, dewPoint, rock);

            allHours.add(new ForecastResponse.HourForecast(
                    h.time().get(i),
                    temp, humidity, wind, precip, prob, cloud, dewPoint,
                    score, ClimbScoreCalculator.label(score), weatherCode
            ));
        }

        // Open-Meteo incluye past_days=3 (lluvia pasada real para los
        // acumulados); al cliente le devolvemos horas desde las 00:00 de hoy.
        int todayStart = Math.max(0, allHours.size() - 7 * 24);
        List<ForecastResponse.HourForecast> hours = List.copyOf(allHours.subList(todayStart, allHours.size()));

        // days
        Map<String, List<Integer>> byDay = new LinkedHashMap<>();
        for (int i = 0; i < hours.size(); i++) {
            byDay.computeIfAbsent(hours.get(i).time().substring(0, 10), k -> new ArrayList<>()).add(i);
        }
        List<ForecastResponse.DayForecast> days = new ArrayList<>();
        for (var entry : byDay.entrySet()) {
            double tMax = -Double.MAX_VALUE, tMin = Double.MAX_VALUE, pTot = 0;
            int sum = 0;
            for (int idx : entry.getValue()) {
                var hf = hours.get(idx);
                tMax = Math.max(tMax, hf.temperature());
                tMin = Math.min(tMin, hf.temperature());
                pTot += hf.precipitation();
                sum += hf.score();
            }
            int avg = sum / entry.getValue().size();
            days.add(new ForecastResponse.DayForecast(
                    entry.getKey(), round1(tMax), round1(tMin), round1(pTot),
                    avg, ClimbScoreCalculator.label(avg)
            ));
        }

        // current with simple factors — sobre la hora actual real, con
        // acumulados de lluvia hacia atrás (el array incluye el pasado)
        int nowIdx = todayStart;
        java.time.LocalDateTime now = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);
        for (int i = todayStart; i < allHours.size(); i++) {
            if (java.time.LocalDateTime.parse(allHours.get(i).time()).isAfter(now)) break;
            nowIdx = i;
        }
        var cur = allHours.get(nowIdx);
        double precip24h = sumPrecipBack(h.precipitation(), nowIdx, 24);
        double precip72h = sumPrecipBack(h.precipitation(), nowIdx, 72);
        double dryThreshold = 6.0 * RockDryingProfile.forRockType(rock).capMult();
        List<ForecastResponse.ScoreFactor> factors = List.of(
                new ForecastResponse.ScoreFactor("TEMPERATURA", Math.round(cur.temperature()) + "°",
                        cur.temperature() >= 5 && cur.temperature() <= 22),
                new ForecastResponse.ScoreFactor("HUMEDAD", Math.round(cur.humidity()) + "%",
                        cur.humidity() < 70),
                new ForecastResponse.ScoreFactor("VIENTO", Math.round(cur.windSpeed()) + " km/h",
                        cur.windSpeed() < 25),
                new ForecastResponse.ScoreFactor("LLUVIA 24H", round1(precip24h) + " mm", precip24h < 1.0),
                new ForecastResponse.ScoreFactor("LLUVIA 72H", round1(precip72h) + " mm", precip72h < 6.0),
                new ForecastResponse.ScoreFactor("SEQUEDAD AIRE",
                        precip72h < 1.0 ? "Roca seca" : "Roca húmeda", precip72h < 1.0)
        );
        ForecastResponse.Current current = new ForecastResponse.Current(
                cur.time(), cur.temperature(), cur.humidity(), cur.windSpeed(),
                cur.precipitation(), cur.precipitationProbability(), cur.cloudCover(),
                cur.dewPoint(), round1(precip24h), round1(precip72h),
                precip72h < dryThreshold,
                GetForecastUseCase.computeHoursToDry(h.precipitation(), nowIdx, dryThreshold),
                cur.score(), cur.scoreLabel(), factors
        );

        // best day
        ForecastResponse.BestDay best = null;
        if (!days.isEmpty()) {
            int idx = 0;
            for (int i = 1; i < days.size(); i++) {
                if (days.get(i).avgScore() > days.get(idx).avgScore()) idx = i;
            }
            var d = days.get(idx);
            best = new ForecastResponse.BestDay(d.date(), d.avgScore(), d.scoreLabel(), idx);
        }

        // optimal window (lógica compartida con GetForecastUseCase)
        ForecastResponse.OptimalWindow win =
                GetForecastUseCase.bestWindowForDate(hours, cur.time().substring(0, 10));

        return new ForecastResponse(
                "loc:" + lat + "," + lon, "Tu ubicación", lat, lon,
                current, hours, days, best, win
        );
    }

    /** Suma la precipitación de las `len` horas anteriores a `end` (exclusivo). */
    private double sumPrecipBack(List<Double> list, int end, int len) {
        double sum = 0;
        for (int i = Math.max(0, end - len); i < Math.min(end, list.size()); i++) sum += list.get(i);
        return sum;
    }

    private static double round1(double v) { return Math.round(v * 10) / 10.0; }
}
