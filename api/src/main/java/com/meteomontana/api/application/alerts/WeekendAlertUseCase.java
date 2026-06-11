package com.meteomontana.api.application.alerts;

import com.meteomontana.api.application.forecast.ForecastResponse;
import com.meteomontana.api.application.forecast.GetForecastUseCase;
import com.meteomontana.api.domain.port.UserRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.WeekendAlertPrefJpaEntity;
import com.meteomontana.api.infrastructure.push.FcmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Evalúa el próximo vie/sáb/dom de las escuelas elegidas (máx 3) y manda un
 * push comparándolas: nota global (media de los 3 días), desglose por día,
 * y aviso de lluvia (en cuántos días llueve y el máximo de mm acumulados).
 */
@Service
public class WeekendAlertUseCase {

    private static final Logger log = LoggerFactory.getLogger(WeekendAlertUseCase.class);
    /** Un día cuenta como "con lluvia" a partir de este acumulado (un chispeo no arruina el día). */
    private static final double RAIN_DAY_MM = 1.0;
    public static final ZoneId MADRID = ZoneId.of("Europe/Madrid");

    private final GetForecastUseCase getForecast;
    private final UserRepository userRepository;
    private final FcmService fcmService;

    public WeekendAlertUseCase(GetForecastUseCase getForecast,
                               UserRepository userRepository,
                               FcmService fcmService) {
        this.getForecast = getForecast;
        this.userRepository = userRepository;
        this.fcmService = fcmService;
    }

    /** Resumen de una escuela para el finde. */
    record SchoolWeekend(String schoolId, String name, int avgScore,
                         List<Integer> dayScores, int rainDays, double maxRainMm) {}

    public void evaluateAndSend(WeekendAlertPrefJpaEntity pref) {
        var user = userRepository.findByUid(pref.getUid()).orElse(null);
        if (user == null || user.getFcmToken() == null || user.getFcmToken().isBlank()) return;

        // Próximo viernes (o hoy si es viernes) + sábado + domingo.
        LocalDate friday = LocalDate.now(MADRID).with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
        List<String> weekendDates = List.of(
                friday.toString(), friday.plusDays(1).toString(), friday.plusDays(2).toString());

        List<SchoolWeekend> results = new ArrayList<>();
        for (String schoolId : pref.getSchoolIds().split(",")) {
            if (schoolId.isBlank()) continue;
            try {
                ForecastResponse fc = getForecast.execute(schoolId.trim());
                List<Integer> scores = new ArrayList<>();
                int rainDays = 0;
                double maxRain = 0;
                for (ForecastResponse.DayForecast d : fc.days()) {
                    if (!weekendDates.contains(d.date())) continue;
                    scores.add(d.avgScore());
                    if (d.precipitationTotal() >= RAIN_DAY_MM) {
                        rainDays++;
                        maxRain = Math.max(maxRain, d.precipitationTotal());
                    }
                }
                if (scores.isEmpty()) continue;
                int avg = (int) Math.round(scores.stream().mapToInt(Integer::intValue).average().orElse(0));
                results.add(new SchoolWeekend(schoolId.trim(), fc.schoolName(), avg, scores, rainDays, maxRain));
            } catch (Exception e) {
                log.warn("weekend alert: forecast failed for school {}: {}", schoolId, e.getMessage());
            }
        }
        if (results.isEmpty()) return;

        results.sort(Comparator.comparingInt(SchoolWeekend::avgScore).reversed());
        SchoolWeekend winner = results.get(0);

        String title = "⛰ Tu finde: gana " + winner.name() + " (" + winner.avgScore() + ")";
        StringBuilder body = new StringBuilder();
        String[] medals = {"🥇", "🥈", "🥉"};
        for (int i = 0; i < results.size(); i++) {
            SchoolWeekend s = results.get(i);
            if (i > 0) body.append('\n');
            body.append(medals[Math.min(i, 2)]).append(' ')
                .append(s.name()).append(' ').append(s.avgScore())
                .append(" — ").append(dayBreakdown(s.dayScores()))
                .append(", ").append(rainSummary(s));
        }

        boolean ok = fcmService.sendToToken(user.getFcmToken(), title, body.toString(),
                Map.of("type", "weekend_alert", "schoolId", winner.schoolId()));
        log.info("weekend alert para {} → {} ({} escuelas)", pref.getUid(), ok ? "enviada" : "FALLO", results.size());
    }

    private static String dayBreakdown(List<Integer> scores) {
        String[] labels = {"V", "S", "D"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < scores.size(); i++) {
            if (i > 0) sb.append(" · ");
            sb.append(labels[Math.min(i, 2)]).append(' ').append(scores.get(i));
        }
        return sb.toString();
    }

    private static String rainSummary(SchoolWeekend s) {
        if (s.rainDays() == 0) return "sin lluvia";
        return "llueve " + s.rainDays() + " de 3 días (máx "
                + Math.round(s.maxRainMm()) + " mm)";
    }
}
