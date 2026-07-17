package com.meteomontana.api.application.social;

import com.meteomontana.api.application.forecast.GetForecastUseCase;
import com.meteomontana.api.domain.model.School;
import com.meteomontana.api.domain.port.SchoolRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.FeedPostJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SchoolBlockJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataFeedPostRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataSchoolBlockRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /** Una escuela en la historia de novedades de la semana. */
    public record NoveltyRow(String school, int blocks, int lines) {}

    /** Historia "novedades de la semana": totales + desglose por escuela. */
    public record NoveltyStory(
            int days, int totalBlocks, int totalLines, int totalSchools,
            List<NoveltyRow> bySchool) {}

    private static final String KIND_NEW_BLOCK = "NEW_BLOCK";
    private static final String KIND_NEW_LINE = "NEW_LINE";

    private final SchoolRepository schools;
    private final GetForecastUseCase forecast;
    private final SpringDataFeedPostRepository feedPosts;
    private final SpringDataSchoolBlockRepository schoolBlocks;

    public SocialStoryService(SchoolRepository schools, GetForecastUseCase forecast,
                              SpringDataFeedPostRepository feedPosts,
                              SpringDataSchoolBlockRepository schoolBlocks) {
        this.schools = schools;
        this.forecast = forecast;
        this.feedPosts = feedPosts;
        this.schoolBlocks = schoolBlocks;
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

    /**
     * Novedades de los últimos {@code days} días: piedras y vías nuevas por
     * escuela + totales. Fuente: posts NEW_BLOCK/NEW_LINE del feed (creados al
     * aprobar). Una piedra nueva (NEW_BLOCK) suma sus vías (líneas de la piedra);
     * una vía suelta (NEW_LINE) suma 1. Ordenado por vías, de más a menos.
     */
    public NoveltyStory novelties(int days, int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(Math.max(1, days));
        List<FeedPostJpaEntity> posts = feedPosts
                .findByKindInAndCreatedAtAfterOrderByCreatedAtDesc(
                        List.of(KIND_NEW_BLOCK, KIND_NEW_LINE), since);

        // Batch-carga de las piedras referenciadas (para contar sus vías) — sin N+1.
        List<String> blockIds = posts.stream()
                .map(FeedPostJpaEntity::getBlockId).filter(java.util.Objects::nonNull).distinct().toList();
        Map<String, SchoolBlockJpaEntity> blocks = new java.util.HashMap<>();
        if (!blockIds.isEmpty()) {
            schoolBlocks.findAllById(blockIds).forEach(b -> blocks.put(b.getId(), b));
        }

        // Agrega por escuela (orden estable por primera aparición → luego reordena).
        Map<String, int[]> agg = new LinkedHashMap<>();   // school -> [blocks, lines]
        for (FeedPostJpaEntity p : posts) {
            String school = p.getSchoolName() != null ? p.getSchoolName() : "—";
            int[] a = agg.computeIfAbsent(school, k -> new int[2]);
            if (KIND_NEW_BLOCK.equals(p.getKind())) {
                a[0]++;   // +1 piedra
                SchoolBlockJpaEntity b = blocks.get(p.getBlockId());
                a[1] += (b != null && b.getLines() != null) ? b.getLines().size() : 0;
            } else { // NEW_LINE
                a[1]++;
            }
        }

        List<NoveltyRow> rows = agg.entrySet().stream()
                .map(e -> new NoveltyRow(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .sorted(Comparator.comparingInt(NoveltyRow::lines).reversed())
                .toList();

        int totBlocks = rows.stream().mapToInt(NoveltyRow::blocks).sum();
        int totLines = rows.stream().mapToInt(NoveltyRow::lines).sum();
        return new NoveltyStory(days, totBlocks, totLines, rows.size(),
                rows.stream().limit(limit).toList());
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
