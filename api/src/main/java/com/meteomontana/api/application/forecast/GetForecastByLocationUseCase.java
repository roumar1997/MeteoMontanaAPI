package com.meteomontana.api.application.forecast;

import com.meteomontana.api.domain.score.ClimbScoreCalculator;
import com.meteomontana.api.domain.score.RockDryingProfile;
import com.meteomontana.api.infrastructure.weather.OpenMeteoClient;
import com.meteomontana.api.infrastructure.weather.OpenMeteoResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * Forecast genérico para una ubicación arbitraria.
 * Útil para el tab Tiempo: "el tiempo en mi ubicación".
 * No requiere escuela — usa caliza como roca por defecto.
 */
@Service
@RequiredArgsConstructor
public class GetForecastByLocationUseCase {

    private final OpenMeteoClient openMeteoClient;

    public ForecastResponse execute(double lat, double lon, String rockType) {
        // Redondeo a ~1km (2 decimales): el tiempo no varía de verdad en ese
        // radio, y así compartimos caché/tabla entre peticiones del mismo sitio
        // (el GPS nunca repite la coordenada exacta) y entre usuarios cercanos.
        // Sin esto, el tab Tiempo NUNCA acertaba en caché (a diferencia de
        // Escuelas, de coordenadas fijas) → cada apertura llamaba en vivo a
        // Open-Meteo y, si limitaba (429), no había ningún dato guardado de esa
        // coordenada exacta para servir de respaldo → error visible al usuario.
        double roundedLat = Math.round(lat * 100.0) / 100.0;
        double roundedLon = Math.round(lon * 100.0) / 100.0;
        OpenMeteoResponse weather = openMeteoClient.fetchForecast(roundedLat, roundedLon);
        String rock = rockType != null ? rockType : "Caliza";

        // Reuso lógica del GetForecastUseCase manualmente porque
        // los métodos privados no son accesibles. Copiamos lo esencial.
        OpenMeteoResponse.HourlyData h = weather.hourly();
        int lookback = RockDryingProfile.forRockType(rock).lookbackHours();
        List<ForecastResponse.HourForecast> hours = new ArrayList<>(h.time().size());

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

            hours.add(new ForecastResponse.HourForecast(
                    h.time().get(i),
                    temp, humidity, wind, precip, prob, cloud, dewPoint,
                    score, ClimbScoreCalculator.label(score), weatherCode
            ));
        }

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

        // current with simple factors.
        // Open-Meteo devuelve el array horario desde las 00:00 GMT de hoy, NO
        // desde la hora actual: hay que localizar la hora presente (findNowIndex)
        // o "ahora" sería la medianoche.
        int nowIndex = findNowIndex(h, weather.utcOffsetSeconds());
        var cur = hours.get(nowIndex);
        double precip24h = sumPrecip(h.precipitation(), nowIndex, 24);
        double precip72h = sumPrecip(h.precipitation(), nowIndex, 72);
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
                // Sin escuela no hay tipo de roca: no estimamos secado.
                precip72h < 1.0, cur.score(), cur.scoreLabel(), factors, null
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

        // optimal window — solo horas presentes/futuras (i >= nowIndex).
        String today = cur.time().substring(0, 10);
        ForecastResponse.OptimalWindow win = null;
        int windowSize = 4;
        List<Integer> todayIdx = new ArrayList<>();
        for (int i = nowIndex; i < hours.size(); i++) {
            if (hours.get(i).time().startsWith(today)) todayIdx.add(i);
        }
        if (todayIdx.size() >= windowSize) {
            int bestStart = 0;
            double bestAvg = -1;
            for (int s = 0; s + windowSize <= todayIdx.size(); s++) {
                double sum = 0;
                for (int k = 0; k < windowSize; k++) sum += hours.get(todayIdx.get(s + k)).score();
                double avg = sum / windowSize;
                if (avg > bestAvg) { bestAvg = avg; bestStart = s; }
            }
            String startTime = hours.get(todayIdx.get(bestStart)).time().substring(11, 16);
            String endTime = hours.get(todayIdx.get(bestStart + windowSize - 1)).time().substring(11, 16);
            win = new ForecastResponse.OptimalWindow(startTime, endTime, (int) bestAvg);
        }

        return new ForecastResponse(
                "loc:" + lat + "," + lon, "Tu ubicación", lat, lon,
                current, hours, days, best, win
        );
    }

    /** Índice de la hora actual en el array local de Open-Meteo (ver findNowIndex de GetForecastUseCase). */
    private int findNowIndex(OpenMeteoResponse.HourlyData h, int utcOffsetSeconds) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.ofTotalSeconds(utcOffsetSeconds));
        int idx = 0;
        for (int i = 0; i < h.time().size(); i++) {
            LocalDateTime t = LocalDateTime.parse(h.time().get(i));
            if (!t.isAfter(now)) idx = i;
            else break;
        }
        return idx;
    }

    private double sumPrecip(List<Double> list, int from, int len) {
        double sum = 0;
        for (int i = from; i < Math.min(from + len, list.size()); i++) sum += list.get(i);
        return sum;
    }

    private static double round1(double v) { return Math.round(v * 10) / 10.0; }
}
