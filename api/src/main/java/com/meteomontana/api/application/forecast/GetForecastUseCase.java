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
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetForecastUseCase {

    private final SchoolRepository schoolRepository;
    private final OpenMeteoClient openMeteoClient;

    public ForecastResponse execute(String schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new SchoolNotFoundException(schoolId));

        OpenMeteoResponse weather = openMeteoClient.fetchForecast(school.getLat(), school.getLon());

        List<ForecastResponse.HourForecast> hours = buildHourlyForecast(weather, school.getRockType());
        List<ForecastResponse.DayForecast>  days  = buildDailyForecast(weather, hours);
        // Open-Meteo (forecast_days=7, sin timezone) devuelve el array horario
        // empezando en las 00:00 GMT de hoy, NO en la hora actual. Calculamos
        // el índice de la hora presente para que "ahora" y la ventana óptima no
        // usen la medianoche por error.
        int nowIndex                              = findNowIndex(weather.hourly(), weather.utcOffsetSeconds());
        ForecastResponse.Current current          = buildCurrent(weather, hours, school.getRockType(), nowIndex);
        ForecastResponse.BestDay bestDay          = pickBestDay(days);
        ForecastResponse.OptimalWindow window     = pickOptimalWindow(hours, nowIndex);

        return new ForecastResponse(
                school.getId(), school.getName(), school.getLat(), school.getLon(),
                current, hours, days, bestDay, window
        );
    }

    // ──────────────────────────────────────────────────────────────────────────
    // HOURLY
    // ──────────────────────────────────────────────────────────────────────────

    /** Serie horaria de temperatura de ROCA (memoria térmica, C1). */
    private double[] rockTempSeries(OpenMeteoResponse weather, String rockType) {
        OpenMeteoResponse.HourlyData h = weather.hourly();
        return com.meteomontana.api.domain.score.RockTemperatureModel.estimate(
                h.temperature(), h.radiation(), h.windSpeed(),
                com.meteomontana.api.domain.score.RockThermalProfile.forRockType(rockType).tauHours());
    }

    private List<ForecastResponse.HourForecast> buildHourlyForecast(
            OpenMeteoResponse weather, String rockType) {

        OpenMeteoResponse.HourlyData h = weather.hourly();
        int lookback = RockDryingProfile.forRockType(rockType).lookbackHours();
        // Memoria térmica: serie de temperatura de la roca (retardo exponencial
        // sobre aire + sol + viento). Con caché antigua sin radiación degrada a
        // roca ≈ aire → ajuste 0 (comportamiento previo).
        double[] rockTemp = rockTempSeries(weather, rockType);
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
                    temp, humidity, wind, precip, prob, cloud, recentRain, dewPoint, rockType,
                    rockTemp[i]);

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
            String rockType,
            int nowIndex) {

        // nowIndex = hora actual dentro del array (ver findNowIndex). Antes se
        // usaba el índice 0 (medianoche), que daba una temperatura "ahora" falsa.
        var cur = hours.get(nowIndex);
        OpenMeteoResponse.HourlyData h = weather.hourly();

        // Lluvia acumulada 24h y 72h hacia atrás. Open-Meteo solo da forecast,
        // así que aproximamos con las próximas N horas desde ahora (no es exacto,
        // pero útil para el UI). En realidad la app debería tener un endpoint con
        // histórico — TODO.
        double precip24h = sumPrecip(h, nowIndex, 24);
        double precip72h = sumPrecip(h, nowIndex, 72);

        boolean dryRock = isDryRock(cur, precip72h, rockType);

        // Factor de ROCA (C1 visible): tipo + estado térmico + inercia.
        double[] rockNow = rockTempSeries(weather, rockType);
        double rockTempNow = nowIndex < rockNow.length ? rockNow[nowIndex] : cur.temperature();
        List<ForecastResponse.ScoreFactor> factors =
                buildFactors(cur, precip24h, precip72h, rockType, rockTempNow);
        ForecastResponse.RockDrying drying = buildDrying(rockType, precip72h, hours);

        return new ForecastResponse.Current(
                cur.time(), cur.temperature(), cur.humidity(), cur.windSpeed(),
                cur.precipitation(), cur.precipitationProbability(), cur.cloudCover(),
                cur.dewPoint(), round1(precip24h), round1(precip72h),
                dryRock, cur.score(), cur.scoreLabel(), factors, drying
        );
    }

    /**
     * Tiempo de secado estimado tras lluvia. Combina cuatro cosas:
     *  - Tipo de roca: base = ~2/3 del lookback del perfil (granito ~8h,
     *    caliza ~12h, conglomerado ~32h, arenisca ~48h en condiciones medias).
     *  - Lluvia reciente: con lluvia fuerte (≥2× el umbral) se alarga un 50%.
     *  - Condiciones de la ventana de secado (viento, sol, temperatura,
     *    humedad), promediadas sobre las próximas horas: más viento / más sol /
     *    más calor / menos humedad → seca antes (ver adjustForConditions).
     *  - Suelo de seguridad por roca: la arenisca pierde ~75% de resistencia
     *    mojada y su interior sigue empapado aunque la superficie seque, así
     *    que nunca baja de 36h por mucho buen tiempo que haga (regla de campo
     *    de los escaladores); el conglomerado nunca baja de 18h.
     *
     * Sigue siendo una heurística (usamos el proxy de lluvia 72h, no histórico
     * real — TODO arriba), pero ahora con los factores que de verdad mandan.
     */
    private ForecastResponse.RockDrying buildDrying(
            String rockType, double precip72h, List<ForecastResponse.HourForecast> hours) {

        RockDryingProfile profile = RockDryingProfile.forRockType(rockType);
        double threshold = 6.0 * profile.capMult(); // mismo umbral que isDryRock
        int baseHours = (int) Math.round(profile.lookbackHours() * 2.0 / 3.0);
        String key = rockType == null ? "" : rockType.toLowerCase();
        boolean sandstone    = key.contains("arenisca");
        boolean conglomerate = key.contains("conglomerado");
        int floorHours = sandstone ? 36 : conglomerate ? 18 : 3;

        boolean wet = precip72h >= threshold;
        if (!wet) {
            // La arenisca es frágil mojada por dentro: avisamos aunque "parezca"
            // seca si ha caído algo de lluvia en la ventana.
            if (sandstone && precip72h >= 1.0) {
                int h = Math.max(adjustForConditions(baseHours, hours), floorHours);
                return new ForecastResponse.RockDrying(false, h,
                        "Arenisca: evita escalar " + h + " h tras lluvia");
            }
            return new ForecastResponse.RockDrying(false, null, null);
        }

        int rainBase = precip72h >= threshold * 2
                ? (int) Math.round(baseHours * 1.5)
                : baseHours;
        int est = Math.max(adjustForConditions(rainBase, hours), floorHours);
        String message = sandstone
                ? "Arenisca: no escalar hasta ~" + est + " h tras la lluvia"
                : "Seca en ~" + est + " h";
        return new ForecastResponse.RockDrying(true, est, message);
    }

    /**
     * Acorta o alarga las horas de secado según las condiciones de la ventana
     * en la que la roca de verdad se seca — no el instante actual: si ahora es
     * de noche no hay sol y engañaría. Promedia viento, nubes, temperatura y
     * humedad sobre las próximas horas y multiplica un factor por cada uno,
     * con tope [0.5, 1.8] para que ninguna combinación se desmadre.
     */
    private int adjustForConditions(int baseHours, List<ForecastResponse.HourForecast> hours) {
        int window = Math.min(Math.min(Math.max(baseHours, 6), 24), hours.size());
        if (window <= 0) return baseHours;

        double wind = 0, cloud = 0, temp = 0, hum = 0;
        for (int i = 0; i < window; i++) {
            var hf = hours.get(i);
            wind  += hf.windSpeed();
            cloud += hf.cloudCover();
            temp  += hf.temperature();
            hum   += hf.humidity();
        }
        wind /= window; cloud /= window; temp /= window; hum /= window;

        // Viento: gran acelerador de la evaporación. Calma → seca más lento.
        double windF = wind >= 30 ? 0.70 : wind >= 20 ? 0.82 : wind >= 10 ? 0.92 : 1.10;
        // Sol: cielo despejado seca rápido; cubierto retiene humedad.
        double sunF  = cloud <= 20 ? 0.80 : cloud <= 50 ? 0.92 : cloud <= 80 ? 1.05 : 1.20;
        // Temperatura: calor evapora; frío alarga.
        double tempF = temp >= 25 ? 0.82 : temp >= 18 ? 0.92 : temp >= 10 ? 1.05 : 1.25;
        // Humedad del aire: seca antes con aire seco; aire húmedo no deja evaporar.
        double humF  = hum  <= 40 ? 0.85 : hum  <= 60 ? 0.95 : hum  <= 80 ? 1.08 : 1.25;

        double mult = Math.max(0.5, Math.min(1.8, windF * sunF * tempF * humF));
        return (int) Math.round(baseHours * mult);
    }

    /**
     * Índice de la hora "ahora" dentro del array horario de Open-Meteo.
     * Con timezone=auto las horas vienen en hora LOCAL del sitio, así que
     * comparamos con la hora local (UTC + offset) y devolvemos la última hora
     * cuyo timestamp ya ha llegado. Si todas son futuras, devuelve 0.
     */
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
            ForecastResponse.HourForecast cur, double precip24h, double precip72h,
            String rockType, double rockTempNow) {

        List<ForecastResponse.ScoreFactor> list = new ArrayList<>();

        // Temperatura óptima: entre 5 y 22°C. Por encima penaliza.
        list.add(new ForecastResponse.ScoreFactor(
                "TEMPERATURA", Math.round(cur.temperature()) + "°",
                cur.temperature() >= 5 && cur.temperature() <= 22
        ));
        // ROCA (C1): tipo, si sigue caliente y su inercia térmica — pedido
        // por Rodrigo («granito y temp tarda no sé cuánto, aún caliente»).
        var rock = com.meteomontana.api.domain.score.RockThermalExplainer.explain(
                rockType, rockTempNow, cur.temperature(),
                com.meteomontana.api.domain.score.RockThermalProfile.forRockType(rockType).tauHours());
        list.add(new ForecastResponse.ScoreFactor(rock.name(), rock.display(), rock.passes()));

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
            List<ForecastResponse.HourForecast> hours, int nowIndex) {

        String today = hours.get(nowIndex).time().substring(0, 10);
        // Buscamos la ventana de 4h consecutivas con mejor score promedio del día
        // actual, pero SOLO entre las horas que aún no han pasado (i >= nowIndex);
        // antes incluía la madrugada ya pasada y proponía ventanas inexistentes.
        int windowSize = 4;
        int bestStart = -1;
        double bestAvg = -1;

        List<Integer> todayIdx = new ArrayList<>();
        for (int i = nowIndex; i < hours.size(); i++) {
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
