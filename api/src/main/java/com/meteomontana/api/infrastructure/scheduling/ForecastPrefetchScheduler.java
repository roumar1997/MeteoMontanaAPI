package com.meteomontana.api.infrastructure.scheduling;

import com.meteomontana.api.domain.port.SchoolRepository;
import com.meteomontana.api.infrastructure.weather.OpenMeteoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Pre-descarga el forecast de TODAS las escuelas cada hora (y al arrancar) con
 * pocas peticiones batch a Open-Meteo, y lo guarda en la tabla forecast_cache.
 * Así las peticiones de los usuarios se sirven de la tabla (instantáneo) y el
 * uso de Open-Meteo es constante (~4 peticiones/hora ≈ 96/día), pasen 10 o
 * 10.000 usuarios. Mata de raíz el 429 por picos de tráfico.
 */
@Component
public class ForecastPrefetchScheduler {

    private static final Logger log = LoggerFactory.getLogger(ForecastPrefetchScheduler.class);

    private final SchoolRepository schoolRepository;
    private final OpenMeteoClient openMeteoClient;

    public ForecastPrefetchScheduler(SchoolRepository schoolRepository,
                                     OpenMeteoClient openMeteoClient) {
        this.schoolRepository = schoolRepository;
        this.openMeteoClient = openMeteoClient;
    }

    /** Cada hora en el minuto 5 (los modelos de Open-Meteo se actualizan ~cada 3h). */
    @Scheduled(cron = "0 5 * * * *", zone = "Europe/Madrid")
    public void hourly() {
        refresh();
    }

    /** Al arrancar (async para no retrasar el boot): deja la tabla caliente ya. */
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        refresh();
    }

    private void refresh() {
        List<double[]> coords = schoolRepository.findAll().stream()
                .map(s -> new double[]{s.getLat(), s.getLon()})
                .toList();
        if (coords.isEmpty()) return;
        log.info("Forecast prefetch: refrescando {} escuelas…", coords.size());
        openMeteoClient.refreshAll(coords);
    }
}
