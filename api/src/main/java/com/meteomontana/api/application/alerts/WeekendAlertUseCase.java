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
    private final com.meteomontana.api.domain.port.SchoolRepository schoolRepository;
    private final FcmService fcmService;

    public WeekendAlertUseCase(GetForecastUseCase getForecast,
                               UserRepository userRepository,
                               com.meteomontana.api.domain.port.SchoolRepository schoolRepository,
                               FcmService fcmService) {
        this.getForecast = getForecast;
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
        this.fcmService = fcmService;
    }

    /** Resumen de una escuela para los días elegidos. */
    record SchoolWeekend(String schoolId, String name, int avgScore,
                         List<Integer> dayScores, List<DayOfWeek> days,
                         int rainDays, double maxRainMm) {}

    public void evaluateAndSend(WeekendAlertPrefJpaEntity pref) {
        var user = userRepository.findByUid(pref.getUid()).orElse(null);
        if (user == null || user.getFcmToken() == null || user.getFcmToken().isBlank()) return;

        // Para cada día de la semana elegido, su próxima ocurrencia dentro de
        // los 7 días empezando hoy (hoy cuenta si está elegido).
        List<Integer> alertDays = com.meteomontana.api.infrastructure.web.WeekendAlertController
                .parseDays(pref.getAlertDays());
        LocalDate today = LocalDate.now(MADRID);
        List<String> targetDates = new ArrayList<>();
        for (int offset = 0; offset < 7; offset++) {
            LocalDate d = today.plusDays(offset);
            if (alertDays.contains(d.getDayOfWeek().getValue())) targetDates.add(d.toString());
        }
        if (targetDates.isEmpty()) return;

        List<String> schoolIds = resolveSchoolIds(pref);
        List<SchoolWeekend> results = new ArrayList<>();
        for (String schoolId : schoolIds) {
            if (schoolId.isBlank()) continue;
            try {
                ForecastResponse fc = getForecast.execute(schoolId.trim());
                List<Integer> scores = new ArrayList<>();
                List<DayOfWeek> daysFound = new ArrayList<>();
                int rainDays = 0;
                double maxRain = 0;
                for (ForecastResponse.DayForecast d : fc.days()) {
                    if (!targetDates.contains(d.date())) continue;
                    scores.add(d.avgScore());
                    daysFound.add(LocalDate.parse(d.date()).getDayOfWeek());
                    if (d.precipitationTotal() >= RAIN_DAY_MM) {
                        rainDays++;
                        maxRain = Math.max(maxRain, d.precipitationTotal());
                    }
                }
                if (scores.isEmpty()) continue;
                int avg = (int) Math.round(scores.stream().mapToInt(Integer::intValue).average().orElse(0));
                results.add(new SchoolWeekend(schoolId.trim(), fc.schoolName(), avg, scores, daysFound, rainDays, maxRain));
            } catch (Exception e) {
                log.warn("weekend alert: forecast failed for school {}: {}", schoolId, e.getMessage());
            }
        }
        if (results.isEmpty()) return;

        results.sort(Comparator.comparingInt(SchoolWeekend::avgScore).reversed());
        // En modo NEARBY se evalúan más escuelas pero solo comparamos las 3 mejores.
        if (results.size() > 3) results = new ArrayList<>(results.subList(0, 3));
        SchoolWeekend winner = results.get(0);

        String title = "⛰ Tus días: gana " + winner.name() + " (" + winner.avgScore() + ")";
        StringBuilder body = new StringBuilder();
        String[] medals = {"🥇", "🥈", "🥉"};
        for (int i = 0; i < results.size(); i++) {
            SchoolWeekend s = results.get(i);
            if (i > 0) body.append('\n');
            body.append(medals[Math.min(i, 2)]).append(' ')
                .append(s.name()).append(' ').append(s.avgScore())
                .append(" — ").append(dayBreakdown(s.dayScores(), s.days()))
                .append(", ").append(rainSummary(s));
        }

        // Data-only: con bloque notification Android en background no ejecuta
        // onMessageReceived y la notificación sale sin icono ni deep link.
        String idsCsv = results.stream().map(SchoolWeekend::schoolId)
                .reduce((a, b) -> a + "," + b).orElse(winner.schoolId());
        boolean ok = fcmService.sendDataToToken(user.getFcmToken(), Map.of(
                "title", title,
                "body", body.toString(),
                "targetType", "compare",
                "targetId", idsCsv));
        log.info("weekend alert para {} → {} ({} escuelas)", pref.getUid(), ok ? "enviada" : "FALLO", results.size());
    }

    /**
     * SCHOOLS → los ids elegidos a mano. NEARBY → hasta 12 escuelas dentro del
     * radio desde la última posición del usuario, las más cercanas primero
     * (cap para no disparar demasiadas consultas de forecast por usuario).
     */
    private List<String> resolveSchoolIds(WeekendAlertPrefJpaEntity pref) {
        if (!"NEARBY".equalsIgnoreCase(pref.getMode())) {
            String csv = pref.getSchoolIds();
            if (csv == null || csv.isBlank()) return List.of();
            return List.of(csv.split(","));
        }
        if (pref.getUserLat() == null || pref.getUserLon() == null || pref.getRadiusKm() == null)
            return List.of();
        double lat = pref.getUserLat(), lon = pref.getUserLon();
        return schoolRepository.findAll().stream()
                .map(s -> Map.entry(s, haversineKm(lat, lon, s.getLat(), s.getLon())))
                .filter(e -> e.getValue() <= pref.getRadiusKm())
                .sorted(Comparator.comparingDouble(Map.Entry::getValue))
                .limit(12)
                .map(e -> e.getKey().getId())
                .toList();
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** Etiquetas L M X J V S D en orden ISO (1=lunes .. 7=domingo). */
    private static final String[] DAY_LABELS = {"L", "M", "X", "J", "V", "S", "D"};

    private static String dayBreakdown(List<Integer> scores, List<DayOfWeek> days) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < scores.size(); i++) {
            if (i > 0) sb.append(" · ");
            sb.append(DAY_LABELS[days.get(i).getValue() - 1]).append(' ').append(scores.get(i));
        }
        return sb.toString();
    }

    private static String rainSummary(SchoolWeekend s) {
        if (s.rainDays() == 0) return "sin lluvia";
        return "llueve " + s.rainDays() + " de " + s.dayScores().size() + " días (máx "
                + Math.round(s.maxRainMm()) + " mm)";
    }
}
