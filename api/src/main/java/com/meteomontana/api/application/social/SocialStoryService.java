package com.meteomontana.api.application.social;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meteomontana.api.application.forecast.GetForecastUseCase;
import com.meteomontana.api.domain.exception.NotFoundException;
import com.meteomontana.api.domain.model.BlockLine;
import com.meteomontana.api.domain.model.FeedPost;
import com.meteomontana.api.domain.model.School;
import com.meteomontana.api.domain.model.SchoolBlock;
import com.meteomontana.api.domain.port.FeedPostRepository;
import com.meteomontana.api.domain.port.SchoolBlockRepository;
import com.meteomontana.api.domain.port.SchoolRepository;
import com.meteomontana.api.domain.port.UserRepository;
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

    /** Una escuela en la historia de condiciones. wind = -1 → desconocido (días
     *  futuros: el forecast diario no trae viento) → el HTML no lo pinta. */
    public record ConditionRow(
            String schoolId, String name, int score,
            boolean dryRock, int temp, int wind) {}

    /** Un día del finde con sus mejores escuelas. label = "VIERNES 18". */
    public record WeekendDay(
            String date, String label, List<ConditionRow> schools) {}

    /** Historia de condiciones: top 3 de HOY + el finde (vie/sáb/dom). */
    public record ConditionsStory(
            String region, List<ConditionRow> today, List<WeekendDay> weekend) {}

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
        /** true si no hay NADA nuevo → n8n NO publica la historia (no mentir con un "0"). */
        @JsonProperty("empty")
        public boolean empty() { return totalBlocks == 0 && totalBoulders == 0 && totalRoutes == 0; }
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

    /** Resumen de una piedra nueva reciente (para que n8n elija cuál destacar).
     *  kindLabel: "BLOQUE NUEVO" / "VÍA NUEVA" (según disciplina). */
    public record RecentBlock(
            long postId, String blockName, String schoolName,
            String discipline, String kindLabel, int lineCount) {}

    /** Lista de piedras nuevas de los últimos {@code days} días (recientes primero).
     *  empty=true → el workflow del miércoles NO publica nada. */
    public record RecentBlocks(int days, boolean empty, List<RecentBlock> blocks) {}

    private static final ObjectMapper JSON = new ObjectMapper();

    private final SchoolRepository schools;
    private final GetForecastUseCase forecast;
    private final FeedPostRepository feedPosts;
    private final SchoolBlockRepository schoolBlocks;
    private final UserRepository users;

    public SocialStoryService(SchoolRepository schools, GetForecastUseCase forecast,
                              FeedPostRepository feedPosts,
                              SchoolBlockRepository schoolBlocks,
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
        FeedPost p = feedPosts.findById(postId).orElse(null);
        if (p == null || !KIND_NEW_BLOCK.equals(p.getKind())) {
            throw new NotFoundException("no es una piedra nueva");
        }
        SchoolBlock block = p.getBlockId() == null ? null
                : schoolBlocks.findById(p.getBlockId()).orElse(null);
        String cover = block != null ? block.getPhotoPath() : null;

        List<NewBlockVia> vias = new ArrayList<>();
        if (block != null && block.getLines() != null) {
            for (BlockLine l : block.getLines()) {
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

        // Fechas del finde de ESTA semana (vie/sáb/dom). Solo las que aún no han
        // pasado y caen dentro del forecast → según avanza la semana, el finde
        // "encoge" a lo que queda (sábado: sáb+dom; domingo: dom).
        java.time.ZoneId madrid = java.time.ZoneId.of("Europe/Madrid");
        java.time.LocalDate today = java.time.LocalDate.now(madrid);
        java.time.LocalDate monday = today.with(
                java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        List<java.time.LocalDate> weekendDates = new ArrayList<>();
        for (int d : new int[]{4, 5, 6}) {              // viernes, sábado, domingo
            java.time.LocalDate wd = monday.plusDays(d);
            if (!wd.isBefore(today)) weekendDates.add(wd);
        }

        List<ConditionRow> todayRows = new ArrayList<>();
        // Por cada fecha del finde: escuela -> fila de ese día (score diario).
        Map<java.time.LocalDate, List<ConditionRow>> byDay = new LinkedHashMap<>();
        for (java.time.LocalDate wd : weekendDates) byDay.put(wd, new ArrayList<>());

        for (School s : inRegion) {
            try {
                var fc = forecast.execute(s.getId());
                var cur = fc.current();
                if (cur != null) {
                    todayRows.add(new ConditionRow(
                            s.getId(), s.getName(), cur.score(), cur.dryRock(),
                            (int) Math.round(cur.temperature()),
                            (int) Math.round(cur.windSpeed())));
                }
                if (fc.days() != null) {
                    for (var day : fc.days()) {
                        java.time.LocalDate dd;
                        try { dd = java.time.LocalDate.parse(day.date()); }
                        catch (Exception e) { continue; }
                        List<ConditionRow> bucket = byDay.get(dd);
                        if (bucket == null) continue;       // no es un día del finde
                        boolean dry = day.precipitationTotal() < 0.2;
                        bucket.add(new ConditionRow(
                                s.getId(), s.getName(), day.avgScore(), dry,
                                (int) Math.round(day.tempMax()), -1));
                    }
                }
            } catch (Exception ignored) {
                // Una escuela sin forecast no tumba la historia.
            }
        }

        todayRows.sort(Comparator.comparingInt(ConditionRow::score).reversed());

        String[] dayNames = {"LUNES", "MARTES", "MIÉRCOLES", "JUEVES", "VIERNES", "SÁBADO", "DOMINGO"};
        List<WeekendDay> weekend = new ArrayList<>();
        for (java.time.LocalDate wd : weekendDates) {
            List<ConditionRow> best = byDay.get(wd);
            best.sort(Comparator.comparingInt(ConditionRow::score).reversed());
            String label = dayNames[wd.getDayOfWeek().getValue() - 1] + " " + wd.getDayOfMonth();
            weekend.add(new WeekendDay(wd.toString(), label,
                    best.stream().limit(2).toList()));    // top 2 por día
        }

        return new ConditionsStory(region, todayRows.stream().limit(3).toList(), weekend);
    }

    /**
     * Piedras nuevas (posts NEW_BLOCK) de los últimos {@code days} días, recientes
     * primero. Lo usa el workflow del MIÉRCOLES: si hay alguna (empty=false),
     * destaca una en una historia; si no, no publica.
     */
    public RecentBlocks recentBlocks(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(Math.max(1, days));
        List<FeedPost> posts = feedPosts.findRecentByKinds(List.of(KIND_NEW_BLOCK), since);

        List<String> blockIds = posts.stream()
                .map(FeedPost::getBlockId).filter(java.util.Objects::nonNull).distinct().toList();
        Map<String, SchoolBlock> blocks = new java.util.HashMap<>();
        schoolBlocks.findByIds(blockIds).forEach(b -> blocks.put(b.getId(), b));

        List<RecentBlock> out = new ArrayList<>();
        for (FeedPost p : posts) {
            SchoolBlock b = blocks.get(p.getBlockId());
            boolean route = b != null && b.getDiscipline() != null
                    && b.getDiscipline() == SchoolBlock.Discipline.ROUTE;
            int lines = b != null && b.getLines() != null ? b.getLines().size() : 0;
            String kindLabel = route ? (lines == 1 ? "VÍA NUEVA" : "VÍAS NUEVAS")
                                     : (lines == 1 ? "BLOQUE NUEVO" : "BLOQUES NUEVOS");
            out.add(new RecentBlock(p.getId(), p.getBlockName(), p.getSchoolName(),
                    route ? "ROUTE" : "BOULDER", kindLabel, lines));
        }
        return new RecentBlocks(days, out.isEmpty(), out);
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
        List<FeedPost> posts = feedPosts.findRecentByKinds(
                List.of(KIND_NEW_BLOCK, KIND_NEW_LINE), since);

        // Batch-carga de las piedras referenciadas (para contar sus vías) — sin N+1.
        List<String> blockIds = posts.stream()
                .map(FeedPost::getBlockId).filter(java.util.Objects::nonNull).distinct().toList();
        Map<String, SchoolBlock> blocks = new java.util.HashMap<>();
        schoolBlocks.findByIds(blockIds).forEach(b -> blocks.put(b.getId(), b));

        // Agrega por escuela distinguiendo BLOQUES de VÍAS (disciplina de la
        // piedra/post). Orden estable por primera aparición → luego reordena.
        Map<String, int[]> agg = new LinkedHashMap<>();   // school -> [piedras, bloques, vias]
        for (FeedPost p : posts) {
            String school = p.getSchoolName() != null ? p.getSchoolName() : "—";
            int[] a = agg.computeIfAbsent(school, k -> new int[3]);
            if (KIND_NEW_BLOCK.equals(p.getKind())) {
                a[0]++;   // +1 piedra
                SchoolBlock b = blocks.get(p.getBlockId());
                if (b != null && b.getLines() != null) {
                    boolean route = b.getDiscipline() == SchoolBlock.Discipline.ROUTE;
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
