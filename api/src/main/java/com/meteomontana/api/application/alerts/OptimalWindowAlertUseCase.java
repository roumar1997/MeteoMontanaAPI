package com.meteomontana.api.application.alerts;

import com.meteomontana.api.application.forecast.ForecastResponse;
import com.meteomontana.api.application.forecast.GetForecastUseCase;
import com.meteomontana.api.domain.port.UserRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataFavoriteRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataWeekendAlertRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.WeekendAlertPrefJpaEntity;
import com.meteomontana.api.domain.port.PushSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Alerta "ventana óptima hoy": si alguna escuela favorita del usuario tiene
 * hoy una ventana óptima cuyo score medio supera su umbral, mandamos un push
 * con la mejor de todas. Máximo un aviso al día (optimal_last_sent).
 */
@Service
public class OptimalWindowAlertUseCase {

    private static final Logger log = LoggerFactory.getLogger(OptimalWindowAlertUseCase.class);
    /** Cap de favoritas a evaluar por usuario para no disparar llamadas a Open-Meteo. */
    private static final int MAX_FAVORITES = 6;

    private final GetForecastUseCase getForecast;
    private final UserRepository userRepository;
    private final SpringDataFavoriteRepository favoriteRepository;
    private final SpringDataWeekendAlertRepository alertRepository;
    private final PushSender fcmService;

    public OptimalWindowAlertUseCase(GetForecastUseCase getForecast,
                                     UserRepository userRepository,
                                     SpringDataFavoriteRepository favoriteRepository,
                                     SpringDataWeekendAlertRepository alertRepository,
                                     PushSender fcmService) {
        this.getForecast = getForecast;
        this.userRepository = userRepository;
        this.favoriteRepository = favoriteRepository;
        this.alertRepository = alertRepository;
        this.fcmService = fcmService;
    }

    /** Mejor candidata del usuario: escuela + ventana de hoy. */
    private record Candidate(String schoolId, String name, ForecastResponse.OptimalWindow window) {}

    public void evaluateAndSend(WeekendAlertPrefJpaEntity pref) {
        LocalDate today = LocalDate.now(WeekendAlertUseCase.MADRID);
        if (today.equals(pref.getOptimalLastSent())) return; // ya avisado hoy

        var user = userRepository.findByUid(pref.getUid()).orElse(null);
        if (user == null) return;

        List<String> favorites = favoriteRepository.findSchoolIdsByUid(pref.getUid());
        if (favorites.isEmpty()) return;
        if (favorites.size() > MAX_FAVORITES) favorites = favorites.subList(0, MAX_FAVORITES);

        Candidate best = null;
        for (String schoolId : favorites) {
            try {
                ForecastResponse fc = getForecast.execute(schoolId);
                ForecastResponse.OptimalWindow w = fc.bestWindow();
                if (w == null || w.avgScore() < pref.getOptimalThreshold()) continue;
                if (best == null || w.avgScore() > best.window().avgScore()) {
                    best = new Candidate(schoolId, fc.schoolName(), w);
                }
            } catch (Exception e) {
                log.warn("optimal alert: forecast failed for school {}: {}", schoolId, e.getMessage());
            }
        }
        if (best == null) return;

        var w = best.window();
        String title = "🌤 Ventana óptima hoy: " + best.name() + " (" + w.avgScore() + ")";
        String body = "De " + w.start() + " a " + w.end() + " — índice " + w.avgScore()
                + ", por encima de tu umbral (" + pref.getOptimalThreshold() + "). ¡Aprovecha!";

        // Data-only (como la alerta de tiempo): el tap abre el detalle de la escuela.
        boolean ok = fcmService.sendDataToUser(user.getUid(), Map.of(
                "title", title,
                "body", body,
                "targetType", "school",
                "targetId", best.schoolId())) > 0;
        if (ok) {
            pref.setOptimalLastSent(today);
            alertRepository.save(pref);
        }
        log.info("optimal window alert para {} → {} ({})", pref.getUid(), ok ? "enviada" : "FALLO", best.name());
    }
}
