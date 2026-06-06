package com.meteomontana.api.application.forecast;

import com.meteomontana.api.domain.model.School;
import com.meteomontana.api.domain.port.SchoolRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Devuelve un score aproximado de hoy + array horario para cada escuela.
 * Usa el GetForecastUseCase pero con caché agresivo a nivel de método
 * para no machacar Open-Meteo.
 *
 * Nota: limitado a top 50 escuelas por defecto para no saturar.
 */
@Service
public class GetTodayScoresUseCase {

    public record SchoolScoreDto(
            String id,
            int todayScore,
            List<Integer> hourlyScores,   // próximas 10 horas
            boolean dryRock,
            double rainMm,                // mm de lluvia ahora (current)
            int rainProb                  // probabilidad % ahora (current)
    ) {}

    private final SchoolRepository schoolRepository;
    private final GetForecastUseCase forecastUseCase;

    public GetTodayScoresUseCase(SchoolRepository schoolRepository,
                                 GetForecastUseCase forecastUseCase) {
        this.schoolRepository = schoolRepository;
        this.forecastUseCase = forecastUseCase;
    }

    /** Devuelve scores para los IDs solicitados (max 30 por call). */
    @Cacheable(value = "today-scores", key = "#ids.toString()")
    public List<SchoolScoreDto> forIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<SchoolScoreDto> out = new ArrayList<>();
        for (String id : ids.stream().limit(50).toList()) {
            School s = schoolRepository.findById(id).orElse(null);
            if (s == null) continue;
            try {
                var fc = forecastUseCase.execute(id);
                int today = fc.current() != null ? fc.current().score() : 0;
                List<Integer> hourly = fc.hours().stream()
                        .limit(10).map(h -> h.score()).toList();
                boolean dry = fc.current() != null && fc.current().dryRock();
                double mm = fc.current() != null ? fc.current().precipitation() : 0.0;
                int prob = fc.current() != null ? fc.current().precipitationProbability() : 0;
                out.add(new SchoolScoreDto(id, today, hourly, dry, mm, prob));
            } catch (Exception e) {
                out.add(new SchoolScoreDto(id, 0, Collections.emptyList(), false, 0.0, 0));
            }
        }
        return out;
    }
}
