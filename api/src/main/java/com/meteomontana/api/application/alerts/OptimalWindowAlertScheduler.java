package com.meteomontana.api.application.alerts;

import com.meteomontana.api.domain.port.AlertPreferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

/**
 * Evalúa la alerta "ventana óptima hoy" cada hora entre las 7:00 y las 11:00
 * (Madrid): si a las 7 aún no hay ventana buena pero el día mejora, se
 * reintenta hasta las 11. El use case garantiza máximo un push al día.
 * Acotamos la franja para no machacar Open-Meteo el resto del día.
 */
@Component
@RequiredArgsConstructor
public class OptimalWindowAlertScheduler {

    private static final Logger log = LoggerFactory.getLogger(OptimalWindowAlertScheduler.class);

    private final AlertPreferenceRepository repository;
    private final OptimalWindowAlertUseCase useCase;

    @Scheduled(cron = "0 0 7-11 * * *", zone = "Europe/Madrid")
    public void run() {
        var enabled = repository.findOptimalEnabled();
        if (enabled.isEmpty()) return;
        log.info("optimal window alerts: evaluando {} usuarios", enabled.size());
        enabled.forEach(pref -> {
            try {
                useCase.evaluateAndSend(pref);
            } catch (Exception e) {
                log.error("optimal window alert falló para {}: {}", pref.uid(), e.getMessage());
            }
        });
    }
}
