package com.meteomontana.api.application.alerts;

import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataWeekendAlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

/**
 * Cada hora en punto busca los usuarios cuya alerta del finde toca ahora
 * (día de la semana + hora, en hora de Madrid) y dispara la evaluación.
 */
@Component
public class WeekendAlertScheduler {

    private static final Logger log = LoggerFactory.getLogger(WeekendAlertScheduler.class);

    private final SpringDataWeekendAlertRepository repository;
    private final WeekendAlertUseCase useCase;

    public WeekendAlertScheduler(SpringDataWeekendAlertRepository repository,
                                 WeekendAlertUseCase useCase) {
        this.repository = repository;
        this.useCase = useCase;
    }

    @Scheduled(cron = "0 0 * * * *", zone = "Europe/Madrid")
    public void run() {
        ZonedDateTime now = ZonedDateTime.now(WeekendAlertUseCase.MADRID);
        var due = repository.findByEnabledTrueAndNotifyDayAndNotifyHour(
                now.getDayOfWeek().getValue(), now.getHour());
        if (due.isEmpty()) return;
        log.info("weekend alerts: {} usuarios a las {}h del día {}", due.size(), now.getHour(), now.getDayOfWeek());
        due.forEach(pref -> {
            try {
                useCase.evaluateAndSend(pref);
            } catch (Exception e) {
                log.error("weekend alert falló para {}: {}", pref.getUid(), e.getMessage());
            }
        });
    }
}
