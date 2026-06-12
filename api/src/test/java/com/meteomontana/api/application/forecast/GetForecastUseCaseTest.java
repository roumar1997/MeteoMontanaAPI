package com.meteomontana.api.application.forecast;

import com.meteomontana.api.domain.model.School;
import com.meteomontana.api.domain.port.SchoolRepository;
import com.meteomontana.api.infrastructure.weather.OpenMeteoClient;
import com.meteomontana.api.infrastructure.weather.OpenMeteoResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifica el manejo de past_days=3: el array de Open-Meteo trae 72h de
 * pasado + 7 días de futuro, pero al cliente se le devuelven solo las horas
 * desde las 00:00 de hoy, y los acumulados/secado se calculan con el pasado real.
 */
class GetForecastUseCaseTest {

    private static final int PAST_HOURS = 72;
    private static final int FUTURE_HOURS = 7 * 24;
    private static final int TOTAL = PAST_HOURS + FUTURE_HOURS;

    /** Respuesta sintética: empieza 72h antes de las 00:00 de hoy (UTC), sin lluvia. */
    private static OpenMeteoResponse syntheticWeather(double[] precipOverrides) {
        LocalDateTime start = LocalDate.now(ZoneOffset.UTC).atStartOfDay().minusHours(PAST_HOURS);
        List<String> time = new ArrayList<>(TOTAL);
        List<Double> temp = new ArrayList<>(), hum = new ArrayList<>(), wind = new ArrayList<>(),
                precip = new ArrayList<>(), dew = new ArrayList<>();
        List<Integer> prob = new ArrayList<>(), cloud = new ArrayList<>(), code = new ArrayList<>();
        for (int i = 0; i < TOTAL; i++) {
            time.add(start.plusHours(i).toString());
            temp.add(15.0); hum.add(50.0); wind.add(5.0); dew.add(5.0);
            precip.add(precipOverrides != null && i < precipOverrides.length ? precipOverrides[i] : 0.0);
            prob.add(0); cloud.add(20); code.add(0);
        }
        return new OpenMeteoResponse(40.0, -3.0, 600,
                new OpenMeteoResponse.HourlyData(time, temp, hum, precip, prob, wind, cloud, dew, code));
    }

    private static GetForecastUseCase useCase(OpenMeteoResponse weather) {
        SchoolRepository schools = mock(SchoolRepository.class);
        when(schools.findById("s1")).thenReturn(Optional.of(
                new School("s1", "Test", "Madrid", "Madrid", "Boulder", "Caliza", 40.0, -3.0, "test")));
        OpenMeteoClient client = mock(OpenMeteoClient.class);
        when(client.fetchForecast(40.0, -3.0)).thenReturn(weather);
        return new GetForecastUseCase(schools, client);
    }

    @Test
    void hoursStartTodayAndPastDaysAreHiddenFromClient() {
        ForecastResponse r = useCase(syntheticWeather(null)).execute("s1");

        String today = LocalDate.now(ZoneOffset.UTC).toString();
        assertEquals(FUTURE_HOURS, r.hours().size());
        assertTrue(r.hours().get(0).time().startsWith(today + "T00:00"));
        assertEquals(7, r.days().size());
        assertEquals(today, r.days().get(0).date());
    }

    @Test
    void currentIsTheRealCurrentHourNotMidnight() {
        ForecastResponse r = useCase(syntheticWeather(null)).execute("s1");

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.HOURS);
        assertEquals(now.toString(), r.current().time());
    }

    @Test
    void dryRockAndZeroHoursToDryWithoutRain() {
        ForecastResponse r = useCase(syntheticWeather(null)).execute("s1");

        assertTrue(r.current().dryRock());
        assertEquals(0, r.current().hoursToDry());
        assertEquals(0.0, r.current().precip72h());
    }

    @Test
    void recentRainMakesRockWetUntilItLeavesThe72hWindow() {
        // 10 mm hace exactamente 1h (caliza: umbral 6.0 mm en 72h).
        int nowFull = PAST_HOURS + LocalDateTime.now(ZoneOffset.UTC).getHour();
        double[] precip = new double[nowFull];
        precip[nowFull - 1] = 10.0;

        ForecastResponse r = useCase(syntheticWeather(precip)).execute("s1");

        assertFalse(r.current().dryRock());
        assertEquals(10.0, r.current().precip24h());
        assertEquals(10.0, r.current().precip72h());
        // Ventana [t-72, t): la lluvia del índice nowFull-1 queda fuera
        // cuando t-72 > nowFull-1, es decir t = nowFull+72 → 72h desde ahora.
        assertEquals(72, r.current().hoursToDry());
    }

    @Test
    void bestWindowForDatePicksTheBest4hRange() {
        List<ForecastResponse.HourForecast> hours = new ArrayList<>();
        // 24 horas del 2026-06-13: score 10 salvo 12:00-15:00 con score 90.
        for (int h = 0; h < 24; h++) {
            int score = (h >= 12 && h <= 15) ? 90 : 10;
            hours.add(new ForecastResponse.HourForecast(
                    String.format("2026-06-13T%02d:00", h),
                    15, 50, 5, 0, 0, 20, 5.0, score, "label", 0));
        }
        ForecastResponse.OptimalWindow w = GetForecastUseCase.bestWindowForDate(hours, "2026-06-13");

        assertNotNull(w);
        assertEquals("12:00", w.start());
        assertEquals("15:00", w.end());
        assertEquals(90, w.avgScore());

        assertNull(GetForecastUseCase.bestWindowForDate(hours, "2026-06-14"));
        assertNull(GetForecastUseCase.bestWindowForDate(Collections.emptyList(), "2026-06-13"));
    }
}
