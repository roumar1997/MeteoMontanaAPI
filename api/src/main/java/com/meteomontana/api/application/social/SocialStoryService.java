package com.meteomontana.api.application.social;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meteomontana.api.application.forecast.GetForecastUseCase;
import com.meteomontana.api.domain.model.School;
import com.meteomontana.api.domain.port.SchoolRepository;
import com.meteomontana.api.domain.port.UserRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.BlockLineJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.FeedPostJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SchoolBlockJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataFeedPostRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataSchoolBlockRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

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

    /** Una escuela en la historia de novedades de la semana.
     *  boulders = líneas de piedras de BLOQUE; routes = líneas de muros de VÍA
     *  (en Cumbre se distinguen SIEMPRE: bloque != vía). lines = suma. */
    public record NoveltyRow(String school, int blocks, int boulders, int routes) {
        public int lines() { return boulders + routes; }
    }

    /** Historia "novedades de la semana": totales + desglose por escuela. */
    public record NoveltyStory(
            int days, int totalBlocks, int totalBoulders, int totalRoutes, int totalSchools,
            List<NoveltyRow> bySchool) {
        public int totalLines() { return totalBoulders + totalRoutes; }
    }

    private static final String KIND_NEW_BLOCK = "NEW_BLOCK";
    private static final String KIND_NEW_LINE = "NEW_LINE";

    /** Una vía de la piedra nueva (con sus puntos normalizados 0..1 para dibujar). */
    public record NewBlockVia(String name, String grade, String startType, List<double[]> points) {}

    /** Historia "piedra nueva" (post automático al aprobar).
     *  discipline: BOULDER → "bloques"; ROUTE → "vías" (SIEMPRE se distinguen). */
    public record NewBlockStory(
            long postId, String blockName, String schoolName, String author,
            String discipline, List<NewBlockVia> vias) {}

    private static final ObjectMapper JSON = new ObjectMapper();

    private final SchoolRepository schools;
    private final GetForecastUseCase forecast;
    private final SpringDataFeedPostRepository feedPosts;
    private final SpringDataSchoolBlockRepository schoolBlocks;
    private final UserRepository users;

    public SocialStoryService(SchoolRepository schools, GetForecastUseCase forecast,
                              SpringDataFeedPostRepository feedPosts,
                              SpringDataSchoolBlockRepository schoolBlocks,
                              UserRepository users) {
        this.schools = schools;
        this.forecast = forecast;
        this.feedPosts = feedPosts;
        this.schoolBlocks = schoolBlocks;
        this.users = users;
    }

    /**
     * Datos de la historia de una piedra nueva a partir de su post NEW_BLOCK.
     * La foto se sirve aparte por {@code /s/p/{postId}/photo}. 404 si el post no
     * existe o no es una piedra nueva.
     */
    public NewBlockStory newBlock(long postId) {
        FeedPostJpaEntity p = feedPosts.findById(postId).orElse(null);
        if (p == null || !KIND_NEW_BLOCK.equals(p.getKind())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "no es una piedra nueva");
        }
        SchoolBlockJpaEntity block = p.getBlockId() == null ? null
                : schoolBlocks.findById(p.getBlockId()).orElse(null);
        String cover = block != null ? block.getPhotoPath() : null;

        List<NewBlockVia> vias = new ArrayList<>();
        if (block != null && block.getLines() != null) {
            for (BlockLineJpaEntity l : block.getLines()) {
                // Solo las vías de la cara PORTADA (misma foto que la portada, o
                // sin foto propia) — coherente con lo que pinta la app.
                boolean coverFace = l.getPhotoPath() == null
                        || (cover != null && cover.equals(l.getPhotoPath()));
                if (!coverFace) continue;
                List<double[]> pts = parsePoints(l.getLinePath());
                if (pts.isEmpty()) continue;
                vias.add(new NewBlockVia(
                        l.getName(), l.getGrade(),
                        l.getStartType() != null ? l.getStartType().name() : null, pts));
            }
        }

        String author = users.findByUid(p.getUserUid())
                .map(u -> u.getUsername() != null ? u.getUsername() : u.getDisplayName())
                .orElse(null);
        String discipline = block != null && block.getDiscipline() != null
                ? block.getDiscipline().name() : "BOULDER";
        return new NewBlockStory(postId, p.getBlockName(), p.getSchoolName(), author, discipline, vias);
    }

    /** Parsea linePath (JSON [{x,y},...]) a puntos normalizados 0..1. */
    private static List<double[]> parsePoints(String linePath) {
        List<double[]> out = new ArrayList<>();
        if (linePath == null || linePath.isBlank()) return out;
        try {
            JsonNode arr = JSON.readTree(linePath);
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    if (n.has("x") && n.has("y")) {
                        out.add(new double[]{n.get("x").asDouble(), n.get("y").asDouble()});
                    }
                }
            }
        } catch (Exception ignored) { }
        return out;
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

        // Agrega por escuela distinguiendo BLOQUES de VÍAS (disciplina de la
        // piedra/post). Orden estable por primera aparición → luego reordena.
        Map<String, int[]> agg = new LinkedHashMap<>();   // school -> [piedras, bloques, vias]
        for (FeedPostJpaEntity p : posts) {
            String school = p.getSchoolName() != null ? p.getSchoolName() : "—";
            int[] a = agg.computeIfAbsent(school, k -> new int[3]);
            if (KIND_NEW_BLOCK.equals(p.getKind())) {
                a[0]++;   // +1 piedra
                SchoolBlockJpaEntity b = blocks.get(p.getBlockId());
                if (b != null && b.getLines() != null) {
                    boolean route = b.getDiscipline() == com.meteomontana.api.domain.model.SchoolBlock.Discipline.ROUTE;
                    if (route) a[2] += b.getLines().size(); else a[1] += b.getLines().size();
                }
            } else { // NEW_LINE: la disciplina viaja en el post
                if ("ROUTE".equalsIgnoreCase(p.getDiscipline())) a[2]++; else a[1]++;
            }
        }

        List<NoveltyRow> rows = agg.entrySet().stream()
                .map(e -> new NoveltyRow(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2]))
                .sorted(Comparator.comparingInt(NoveltyRow::lines).reversed())
                .toList();

        int totBlocks = rows.stream().mapToInt(NoveltyRow::blocks).sum();
        int totBoulders = rows.stream().mapToInt(NoveltyRow::boulders).sum();
        int totRoutes = rows.stream().mapToInt(NoveltyRow::routes).sum();
        return new NoveltyStory(days, totBlocks, totBoulders, totRoutes, rows.size(),
                rows.stream().limit(limit).toList());
    }

    /**
     * Regiones "publicables": comunidades con al menos {@code minSchools}
     * escuelas, EXCLUYENDO strings ambiguos (multi-región con "/" y provincias
     * sueltas) — para que el bucle de n8n solo genere historias con sentido.
     * Ordenadas de más a menos escuelas.
     */
    public List<String> regions(int minSchools) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (School s : schools.findAll()) {
            String r = s.getRegion();
            if (r == null || r.isBlank()) continue;
            if (r.contains("/")) continue;                    // "Aragón / Cataluña"
            counts.merge(r.trim(), 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .filter(e -> e.getValue() >= Math.max(1, minSchools))
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();
    }

    private static String norm(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");   // quita acentos
        return n.trim().toLowerCase();
    }
}
