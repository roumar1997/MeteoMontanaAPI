package com.meteomontana.api.application.social;

import com.meteomontana.api.application.forecast.GetForecastUseCase;
import com.meteomontana.api.domain.model.School;
import com.meteomontana.api.domain.port.SchoolRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Datos para las historias/posts automáticos de Instagram (proyecto n8n).
 * Solo lectura de datos ya públicos (catálogo + forecast). El render a imagen
 * lo hace fuera (n8n sobre el HTML que sirve {@code SocialStoryController}).
 */
@Service
public class SocialStoryService {

    /** Una escuela en la historia de condiciones diarias. */
    public record ConditionRow(
            String schoolId, String name, int score,
            boolean dryRock, int temp, int wind) {}

    /** Historia "condiciones de hoy" de una comunidad autónoma. */
    public record ConditionsStory(
            String region, List<ConditionRow> schools) {}

    private final SchoolRepository schools;
    private final GetForecastUseCase forecast;

    public SocialStoryService(SchoolRepository schools, GetForecastUseCase forecast) {
        this.schools = schools;
        this.forecast = forecast;
    }

    /**
     * Top {@code limit} escuelas de {@code region} por índice de HOY, de mejor
     * a peor. La región se compara sin acentos ni mayúsculas para tolerar
     * variantes ("Cataluña"/"cataluna"). Escuelas cuyo forecast falla se omiten.
     */
    public ConditionsStory conditions(String region, int limit) {
        String target = norm(region);
        List<School> inRegion = schools.findAll().stream()
                .filter(s -> s.getRegion() != null && norm(s.getRegion()).equals(target))
                .toList();

        List<ConditionRow> rows = new ArrayList<>();
        for (School s : inRegion) {
            try {
                var fc = forecast.execute(s.getId());
                var cur = fc.current();
                if (cur == null) continue;
                rows.add(new ConditionRow(
                        s.getId(), s.getName(), cur.score(), cur.dryRock(),
                        (int) Math.round(cur.temperature()),
                        (int) Math.round(cur.windSpeed())));
            } catch (Exception ignored) {
                // Una escuela sin forecast no tumba la historia.
            }
        }
        rows.sort(Comparator.comparingInt(ConditionRow::score).reversed());
        return new ConditionsStory(region, rows.stream().limit(limit).toList());
    }

    /** Nombre canónico de la región tal cual está en el catálogo (para títulos). */
    public String canonicalRegion(String region) {
        String target = norm(region);
        return schools.findAll().stream()
                .map(School::getRegion)
                .filter(r -> r != null && norm(r).equals(target))
                .findFirst().orElse(region);
    }

    /** Lista de regiones con al menos una escuela (para saber qué historias generar). */
    public List<String> regions() {
        return schools.findAll().stream()
                .map(School::getRegion)
                .filter(r -> r != null && !r.isBlank())
                .distinct().sorted().toList();
    }

    private static String norm(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");   // quita acentos
        return n.trim().toLowerCase();
    }
}
