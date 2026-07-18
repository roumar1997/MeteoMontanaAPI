package com.meteomontana.api.application.social;

import com.meteomontana.api.application.social.SocialStoryService.ConditionRow;
import com.meteomontana.api.application.social.SocialStoryService.ConditionsStory;
import com.meteomontana.api.application.social.SocialStoryService.NewBlockStory;
import com.meteomontana.api.application.social.SocialStoryService.NewBlockVia;
import com.meteomontana.api.application.social.SocialStoryService.NoveltyRow;
import com.meteomontana.api.application.social.SocialStoryService.NoveltyStory;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Renderiza las historias sociales (1080x1920) a HTML autocontenido con la
 * estética Cumbre. n8n captura este HTML a PNG con un Chrome headless.
 *
 * Todo inline (logo SVG, badges SVG, estilos) → sin dependencias externas
 * salvo las Google Fonts (Chrome las carga). Paleta y layout = maquetas
 * aprobadas por Rodrigo (fondo papel, radar, serif, terracota).
 */
public final class SocialStoryHtml {

    private SocialStoryHtml() {}

    private static final Locale ES = Locale.forLanguageTag("es-ES");
    private static final String PAPER = "#F2EFE8";
    private static final String TERRA = "#C0532B";
    private static final String INK = "#1A1A1A";
    private static final String GREEN = "#4E8B57";

    /** Logo Cumbre (montaña + radar + sol) como SVG inline. */
    private static final String LOGO = """
        <svg width="96" height="96" viewBox="0 0 100 100">
          <circle cx="50" cy="50" r="50" fill="#EDE8DF"/>
          <g stroke="#B98A4B" stroke-width="1.4" fill="none" opacity="0.55">
            <circle cx="50" cy="50" r="16"/><circle cx="50" cy="50" r="27"/><circle cx="50" cy="50" r="38"/></g>
          <circle cx="66" cy="34" r="7" fill="#C0532B"/>
          <path d="M0 100 L28 55 L44 78 L58 40 L74 66 L100 100 Z" fill="#9A9A96"/>
          <path d="M58 40 L74 66 L44 66 Z" fill="#1A1A1A"/>
          <path d="M18 78 L44 78 L58 62 L74 100 L0 100 Z" fill="#1A1A1A"/>
          <path d="M52 50 L58 40 L64 50 Z" fill="#F2EFE8"/>
        </svg>""";

    /** Badges de tienda (aprox. oficiales) como SVG inline. */
    private static final String BADGES = """
        <div style="display:flex;gap:24px;justify-content:center">
          <div style="height:92px;background:#000;border-radius:18px;display:flex;align-items:center;gap:16px;padding:0 32px">
            <svg width="40" height="40" viewBox="0 0 24 24" fill="#fff"><path d="M17.05 12.04c-.03-2.86 2.34-4.23 2.44-4.3-1.33-1.95-3.4-2.22-4.14-2.25-1.76-.18-3.44 1.04-4.33 1.04-.89 0-2.27-1.02-3.73-.99-1.92.03-3.69 1.12-4.68 2.84-2 3.46-.51 8.58 1.43 11.39.95 1.37 2.08 2.91 3.56 2.86 1.43-.06 1.97-.92 3.7-.92 1.72 0 2.21.92 3.72.89 1.54-.03 2.51-1.4 3.45-2.78 1.09-1.6 1.54-3.15 1.56-3.23-.03-.01-2.99-1.15-3.02-4.56zM14.2 3.66c.79-.96 1.32-2.29 1.17-3.61-1.14.05-2.51.76-3.32 1.71-.73.85-1.37 2.2-1.2 3.5 1.27.1 2.57-.65 3.35-1.6z"/></svg>
            <div style="line-height:1.1"><div style="font-size:15px;color:#fff;font-family:Inter">Consíguelo en el</div><div style="font-size:30px;color:#fff;font-family:Inter;font-weight:600">App Store</div></div>
          </div>
          <div style="height:92px;background:#000;border-radius:18px;display:flex;align-items:center;gap:16px;padding:0 32px">
            <svg width="36" height="40" viewBox="0 0 24 26"><path d="M1 1 L13 13 L1 25 Z" fill="#00D0FF"/><path d="M1 1 L18 10 L13 13 Z" fill="#00F076"/><path d="M1 25 L18 16 L13 13 Z" fill="#FF3A44"/><path d="M18 10 L23 13 L18 16 L13 13 Z" fill="#FFC900"/></svg>
            <div style="line-height:1.1"><div style="font-size:15px;color:#fff;font-family:Inter">DESCARGAR EN</div><div style="font-size:30px;color:#fff;font-family:Inter;font-weight:600">Google Play</div></div>
          </div>
        </div>""";

    /** Anillos de radar (esquina superior derecha) sobre fondo papel. */
    private static final String RADAR = radar("#C0532B");
    /** Radar sobre fondo oscuro/terracota (anillos negros). */
    private static final String RADAR_DARK = radar("#000000");

    private static String radar(String color) {
        return ("""
            <svg style="position:absolute;top:-260px;right:-260px;width:900px;height:900px" viewBox="0 0 900 900">
              <g fill="none" stroke="%s">
                <circle cx="450" cy="450" r="150" stroke-opacity="0.13" stroke-width="3"/>
                <circle cx="450" cy="450" r="255" stroke-opacity="0.11" stroke-width="3"/>
                <circle cx="450" cy="450" r="355" stroke-opacity="0.09" stroke-width="3"/>
                <circle cx="450" cy="450" r="450" stroke-opacity="0.07" stroke-width="3"/></g>
            </svg>""").formatted(color);
    }

    /** Badges de tienda sobre fondo oscuro/terracota (caja #1A1A1A en vez de #000). */
    private static final String BADGES_DARK = BADGES.replace("background:#000", "background:#1A1A1A");

    // ─────────────────────────────────────────────── condiciones diarias

    public static String conditions(ConditionsStory story) {
        // Tarjetas del finde (vie/sáb/dom), cada una con sus 2 mejores escuelas.
        StringBuilder days = new StringBuilder();
        for (var wd : story.weekend()) {
            StringBuilder best = new StringBuilder();
            if (wd.schools().isEmpty()) {
                best.append("<div style=\"font-family:Inter;font-size:26px;color:#A79F90;margin-top:14px\">—</div>");
            }
            for (ConditionRow s : wd.schools()) {
                String col = s.score() >= 70 ? GREEN : (s.score() >= 45 ? TERRA : "#B23B3B");
                best.append("""
                    <div style="display:flex;align-items:center;justify-content:space-between;margin-top:16px">
                      <div style="font-family:'Source Serif 4',serif;font-weight:600;font-size:30px;color:%s;line-height:1.05;max-width:70%%">%s</div>
                      <div style="font-family:'Source Serif 4',serif;font-weight:700;font-size:44px;color:%s">%d</div>
                    </div>"""
                    .formatted(INK, esc(shortRegion(s.name())), col, s.score()));
            }
            days.append("""
                <div style="flex:1;background:#FAF8F3;border:3px solid #E2DCD2;border-radius:26px;padding:26px 28px">
                  <div style="font-family:'JetBrains Mono',monospace;font-weight:700;font-size:22px;letter-spacing:3px;color:%s">%s</div>
                  %s
                </div>"""
                .formatted(TERRA, wd.label(), best));
        }

        // Franja "hoy": top 3 de ahora mismo, compacto.
        StringBuilder todayRows = new StringBuilder();
        for (ConditionRow s : story.today()) {
            String col = s.score() >= 70 ? GREEN : (s.score() >= 45 ? TERRA : "#B23B3B");
            String dot = s.dryRock() ? GREEN : "#B23B3B";
            todayRows.append("""
                <div style="display:flex;align-items:center;padding:18px 0;border-bottom:2px solid #ECE6DA">
                  <div style="flex:1">
                    <div style="font-family:'Source Serif 4',serif;font-weight:600;font-size:38px;color:%s;line-height:1.05">%s</div>
                    <div style="font-family:Inter;font-size:23px;color:#6B6B6B;margin-top:4px"><span style="display:inline-block;width:13px;height:13px;border-radius:50%%;background:%s;margin-right:8px"></span>Roca %s · %d° · viento %d km/h</div>
                  </div>
                  <div style="text-align:right"><span style="font-family:'Source Serif 4',serif;font-weight:700;font-size:56px;color:%s">%d</span><span style="font-family:'JetBrains Mono',monospace;font-size:22px;color:#8A857B">/100</span></div>
                </div>"""
                .formatted(INK, esc(shortRegion(s.name())), dot,
                        s.dryRock() ? "seca" : "húmeda", s.temp(), s.wind(), col, s.score()));
        }

        String body = """
            <div style="padding:96px 90px 0;position:relative">
              <div style="display:flex;align-items:center;gap:22px">%s
                <div><div style="font-family:'Source Serif 4',serif;font-weight:700;font-size:40px;color:%s;line-height:1">Cumbre</div>
                <div style="font-family:'JetBrains Mono',monospace;font-weight:500;font-size:19px;letter-spacing:5px;color:#8A857B">METEO PARA ESCALAR</div></div>
              </div>
              <div style="font-family:'JetBrains Mono',monospace;font-weight:700;font-size:26px;letter-spacing:7px;color:%s;margin-top:90px">EL FINDE EN %s</div>
              <div style="font-family:'Source Serif 4',serif;font-weight:700;font-size:94px;line-height:1;color:%s;margin-top:20px;letter-spacing:-2px">¿Dónde escalar<br>este finde?</div>
              <div style="font-family:Inter;font-size:29px;color:#6B6B6B;margin-top:20px">Las mejores escuelas para cada día, según el índice de Cumbre.</div>
              <div style="display:flex;gap:22px;margin-top:44px">%s</div>
              <div style="margin-top:44px">
                <div style="font-family:'JetBrains Mono',monospace;font-weight:700;font-size:23px;letter-spacing:5px;color:%s;margin-bottom:6px">HOY, AHORA MISMO</div>
                %s
              </div>
            </div>
            <div style="position:absolute;bottom:80px;left:90px;right:90px">
              <div style="font-family:'JetBrains Mono',monospace;font-weight:700;font-size:22px;letter-spacing:5px;color:#8A857B;text-align:center;margin-bottom:26px">DESCÁRGALA GRATIS</div>
              %s
            </div>"""
            .formatted(LOGO, INK, TERRA, up(shortRegion(story.region())), INK,
                    days, TERRA, todayRows, BADGES);

        return page(PAPER, RADAR + body);
    }

    // ─────────────────────────────────────────────── novedades de la semana

    public static String novelties(NoveltyStory story) {
        int max = story.bySchool().stream().mapToInt(NoveltyRow::lines).max().orElse(1);
        StringBuilder rows = new StringBuilder();
        for (NoveltyRow r : story.bySchool()) {
            int pct = max > 0 ? Math.round(r.lines() * 100f / max) : 0;
            rows.append("""
                <div style="margin-bottom:22px">
                  <div style="display:flex;justify-content:space-between;align-items:baseline">
                    <span style="font-family:'Source Serif 4',serif;font-weight:600;font-size:40px;color:%s">%s</span>
                    <span style="font-family:Inter;font-size:27px;color:#6B6B6B"><b style="color:%s;font-family:'Source Serif 4',serif;font-size:34px">%d</b> %s</span>
                  </div>
                  <div style="height:14px;background:#E7E1D6;border-radius:8px;margin-top:12px;overflow:hidden"><div style="height:100%%;width:%d%%;background:%s;border-radius:8px"></div></div>
                </div>"""
                .formatted(INK, esc(r.school()), TERRA, r.lines(),
                        noveltyBreakdown(r), pct, TERRA));
        }

        String body = """
            <div style="padding:96px 90px 0;position:relative">
              <div style="display:flex;align-items:center;gap:22px">%s
                <div><div style="font-family:'Source Serif 4',serif;font-weight:700;font-size:40px;color:%s;line-height:1">Cumbre</div>
                <div style="font-family:'JetBrains Mono',monospace;font-weight:500;font-size:19px;letter-spacing:5px;color:#8A857B">METEO PARA ESCALAR</div></div>
              </div>
              <div style="font-family:'JetBrains Mono',monospace;font-weight:700;font-size:26px;letter-spacing:7px;color:%s;margin-top:80px">NOVEDADES DE LA SEMANA</div>
              <div style="font-family:'Source Serif 4',serif;font-weight:700;font-size:92px;line-height:0.98;color:%s;margin-top:20px;letter-spacing:-2px">La guía no para<br>de crecer<span style="color:%s">.</span></div>
              <div style="display:flex;gap:26px;margin-top:52px">
                <div style="flex:1;background:#FAF8F3;border:3px solid #E2DCD2;border-radius:26px;padding:30px 34px"><div style="font-family:'Source Serif 4',serif;font-weight:700;font-size:96px;color:%s;line-height:1">%d</div><div style="font-family:'JetBrains Mono',monospace;font-size:22px;letter-spacing:2px;color:#8A857B;margin-top:6px">BLOQUES NUEVOS</div></div>
                <div style="flex:1;background:#FAF8F3;border:3px solid #E2DCD2;border-radius:26px;padding:30px 34px"><div style="font-family:'Source Serif 4',serif;font-weight:700;font-size:96px;color:%s;line-height:1">%d</div><div style="font-family:'JetBrains Mono',monospace;font-size:22px;letter-spacing:2px;color:#8A857B;margin-top:6px">VÍAS NUEVAS</div></div>
                <div style="flex:1;background:#FAF8F3;border:3px solid #E2DCD2;border-radius:26px;padding:30px 34px"><div style="font-family:'Source Serif 4',serif;font-weight:700;font-size:96px;color:%s;line-height:1">%d</div><div style="font-family:'JetBrains Mono',monospace;font-size:22px;letter-spacing:2px;color:#8A857B;margin-top:6px">ESCUELAS</div></div>
              </div>
              <div style="font-family:Inter;font-size:27px;color:#6B6B6B;margin-top:44px;margin-bottom:24px">Dónde se ha crecido:</div>
              %s
            </div>
            <div style="position:absolute;bottom:96px;left:90px;right:90px">
              <div style="font-family:'JetBrains Mono',monospace;font-weight:700;font-size:22px;letter-spacing:5px;color:#8A857B;text-align:center;margin-bottom:26px">AÑADE TU ZONA · DESCARGA CUMBRE</div>
              %s
            </div>"""
            .formatted(LOGO, INK, TERRA, INK, TERRA,
                    TERRA, story.totalBoulders(), INK, story.totalRoutes(), GREEN, story.totalSchools(),
                    rows, BADGES);

        return page(PAPER, RADAR + body);
    }

    // ─────────────────────────────────────────────── ranking de aportadores

    /** Ranking de aportadores. {@code weekly}=true → "Esta semana"; si no, global.
     *  Fondo TERRACOTA para diferenciarlo del resto (crema). */
    public static String contributors(
            List<com.meteomontana.api.application.community.GetTopContributorsUseCase.TopContributorDto> people,
            boolean weekly) {
        // Sin podio, sin números, sin ranking: TODOS los que aportan salen como
        // iguales, en chips del mismo tamaño. La guía la hacemos entre todos.
        StringBuilder chips = new StringBuilder();
        for (var p : people) {
            String name = p.displayName() != null && !p.displayName().isBlank()
                    ? p.displayName() : (p.username() != null ? "@" + p.username() : "Escalador");
            chips.append("""
                <div style="background:rgba(255,255,255,0.14);border:2px solid rgba(255,255,255,0.28);border-radius:999px;padding:18px 30px;font-family:'Source Serif 4',serif;font-weight:600;font-size:38px;color:#fff">%s</div>"""
                .formatted(esc(name)));
        }

        String eyebrow = weekly ? "ESTA SEMANA HAN APORTADO" : "GRACIAS POR APORTAR";
        String title = "La guía la<br>hacemos<br>entre todos";

        String body = """
            <div style="padding:96px 90px 0;position:relative">
              <div style="display:flex;align-items:center;gap:22px">%s
                <div><div style="font-family:'Source Serif 4',serif;font-weight:700;font-size:40px;color:#fff;line-height:1">Cumbre</div>
                <div style="font-family:'JetBrains Mono',monospace;font-weight:500;font-size:19px;letter-spacing:5px;color:rgba(255,255,255,0.6)">METEO PARA ESCALAR</div></div>
              </div>
              <div style="font-family:'JetBrains Mono',monospace;font-weight:700;font-size:26px;letter-spacing:6px;color:#F2E4D0;margin-top:80px">%s</div>
              <div style="font-family:'Source Serif 4',serif;font-weight:700;font-size:88px;line-height:0.98;color:#fff;margin-top:20px;letter-spacing:-2px">%s<span style="color:%s">.</span></div>
              <div style="font-family:Inter;font-size:28px;color:#F2E4D0;margin-top:22px">Cada piedra, vía y sector que suben estos escaladores es de toda la comunidad. Todos sumamos igual.</div>
              <div style="margin-top:48px;display:flex;flex-wrap:wrap;gap:20px">%s</div>
            </div>
            <div style="position:absolute;bottom:96px;left:90px;right:90px">
              <div style="font-family:'JetBrains Mono',monospace;font-weight:700;font-size:22px;letter-spacing:5px;color:rgba(255,255,255,0.65);text-align:center;margin-bottom:26px">SUMA TÚ TAMBIÉN · DESCARGA CUMBRE</div>
              %s
            </div>"""
            .formatted(LOGO, eyebrow, title, INK, chips, BADGES_DARK);

        return page(TERRA, RADAR_DARK + body);
    }

    // ───────────────────────────────────────────────────── piedra nueva

    /** Color de una vía según su grado (MISMO mapeo que la app: gradeArgb en
     *  TopoRenderer.kt). dark=true → línea clara (blanca), necesita halo negro
     *  y texto negro. dashed=true → proyecto (sin grado). */
    private record LineStyle(String color, boolean dark, boolean dashed) {}

    private static LineStyle gradeStyle(String grade) {
        String g = (grade == null ? "" : grade.trim().toUpperCase(Locale.ROOT));
        if (g.isEmpty() || g.equals("PROY") || g.equals("PROYECTO") || g.equals("?")) {
            return new LineStyle("#FF4FA3", false, true);   // proyecto: rosa punteado
        }
        var m = java.util.regex.Pattern.compile("^([3-9])([ABCD])?(\\+)?$").matcher(g);
        if (!m.matches()) return new LineStyle("#FF4FA3", false, true);
        int num = Integer.parseInt(m.group(1));
        int letter = switch (m.group(2) == null ? "A" : m.group(2)) {
            case "B" -> 1; case "C" -> 2; case "D" -> 3; default -> 0;
        };
        int plus = "+".equals(m.group(3)) ? 1 : 0;
        int score = num * 100 + letter * 10 + plus;
        if (score <= 521) return new LineStyle("#FFFFFF", true, false);   // 3-5+  blanco
        if (score <= 611) return new LineStyle("#1FA84E", false, false);  // 6a-6b verde
        if (score <= 621) return new LineStyle("#1D6DD6", false, false);  // 6c    azul
        if (score <= 701) return new LineStyle("#8E3FBF", false, false);  // 6c+-7a morado
        if (score <= 721) return new LineStyle("#D62828", false, false);  // 7a+-7b rojo
        return new LineStyle("#111111", false, false);                    // 7b+    negro
    }

    public static String newBlock(NewBlockStory story) {
        // Título: nombre de la piedra si es "de verdad" (no solo un número);
        // si no, "N vías nuevas en {escuela}".
        String block = story.blockName();
        boolean realName = block != null && !block.isBlank() && !block.matches("\\d+");
        int n = story.vias().size();
        boolean route = "ROUTE".equalsIgnoreCase(story.discipline());
        String title = realName
                ? esc(block)
                : n + (route ? (n == 1 ? " vía nueva" : " vías nuevas")
                             : (n == 1 ? " bloque nuevo" : " bloques nuevos"));
        String eyebrow = route ? (n == 1 ? "VÍA NUEVA" : "VÍAS NUEVAS")
                               : (n == 1 ? "BLOQUE NUEVO" : "BLOQUES NUEVOS");

        // Líneas (SVG viewBox 0..1000, preserveAspectRatio none = alinea con la
        // foto estirada) + badges HTML posicionados por %. ESTILO de la app
        // 2.19: todas discontinuas, tramos compartidos a FRANJAS (cada vía su
        // color con fase distinta) y badges coincidentes en ABANICO.
        final double STRIPE = 34;
        java.util.Map<String, java.util.List<Integer>> shared = new java.util.HashMap<>();
        {
            int li = 0;
            for (NewBlockVia v : story.vias()) {
                List<double[]> pts = v.points();
                for (int i = 0; i + 1 < pts.size(); i++) {
                    String k = segKey(pts.get(i), pts.get(i + 1));
                    var set = shared.computeIfAbsent(k, x -> new java.util.ArrayList<>());
                    if (!set.contains(li)) set.add(li);
                }
                li++;
            }
            shared.values().removeIf(l -> l.size() < 2);
            shared.values().forEach(java.util.Collections::sort);
        }
        java.util.List<Double> startFan = fanOffsets(
                story.vias().stream().map(v -> v.points().isEmpty() ? null : v.points().get(0)).toList(), 0.046);
        java.util.List<Double> endFan = fanOffsets(
                story.vias().stream().map(v -> v.points().isEmpty() ? null : v.points().get(v.points().size() - 1)).toList(), 0.052);

        StringBuilder paths = new StringBuilder();
        StringBuilder badges = new StringBuilder();
        StringBuilder list = new StringBuilder();
        int idx = 0;
        for (NewBlockVia v : story.vias()) {
            LineStyle st = gradeStyle(v.grade());
            String color = st.color();
            List<double[]> pts = v.points();
            // Rachas propias/compartidas (mismo troceo que renderTopo de la app).
            int rs = 0;
            java.util.List<Integer> cur = pts.size() > 1
                    ? shared.getOrDefault(segKey(pts.get(0), pts.get(1)), java.util.List.of())
                    : java.util.List.of();
            for (int i = 1; i <= pts.size() - 1; i++) {
                java.util.List<Integer> s2 = (i < pts.size() - 1)
                        ? shared.getOrDefault(segKey(pts.get(i), pts.get(i + 1)), java.util.List.of())
                        : null;
                if (s2 == null || !s2.equals(cur)) {
                    appendRun(paths, pts.subList(rs, i + 1), color, st.dark(), cur, idx, STRIPE);
                    rs = i;
                    if (s2 != null) cur = s2;
                }
            }
            // Badge numérico en el primer punto (abanico si coincide con otras).
            double[] first = pts.get(0);
            badges.append(badge(first[0] + startFan.get(idx), first[1], String.valueOf(idx + 1), color, st.dark(), 30));
            // Etiqueta de inicio en el último punto (abanico si coincide).
            String label = startLabel(v.startType());
            if (label != null && pts.size() > 1) {
                double[] last = pts.get(pts.size() - 1);
                badges.append(badge(last[0] + endFan.get(idx), last[1], label, color, st.dark(), 26));
            }
            // Fila de la lista.
            String grade = v.grade() != null && !v.grade().isBlank()
                    ? "<span style=\"font-family:'JetBrains Mono',monospace;font-weight:700;font-size:34px;color:" + TERRA + "\">" + esc(v.grade()) + "</span>" : "";
            String numText = st.dark() ? INK : "#fff";
            String numBorder = st.dark() ? ";border:2px solid #1A1A1A" : "";
            list.append("""
                <div style="display:flex;align-items:center;gap:20px">
                  <div style="width:52px;height:52px;border-radius:50%%;background:%s;color:%s%s;font-family:'Source Serif 4',serif;font-weight:700;font-size:30px;display:flex;align-items:center;justify-content:center">%d</div>
                  <div style="flex:1;font-family:'Source Serif 4',serif;font-size:38px;color:%s">%s</div>%s
                </div>"""
                .formatted(color, numText, numBorder, idx + 1, INK, esc(v.name() != null ? v.name() : "Vía " + (idx + 1)), grade));
            idx++;
        }

        String author = story.author() != null && !story.author().isBlank()
                ? "aportada por <b style=\"color:" + INK + ";font-weight:600\">@" + esc(story.author()) + "</b>"
                : "aportada por la comunidad";

        String body = """
            <div style="padding:80px 90px 0;position:relative">
              <div style="display:flex;align-items:center;gap:22px">%s
                <div><div style="font-family:'Source Serif 4',serif;font-weight:700;font-size:38px;color:%s;line-height:1">Cumbre</div>
                <div style="font-family:'JetBrains Mono',monospace;font-weight:500;font-size:18px;letter-spacing:5px;color:#8A857B">METEO PARA ESCALAR</div></div>
              </div>
              <div style="font-family:'JetBrains Mono',monospace;font-weight:700;font-size:24px;letter-spacing:6px;color:%s;margin-top:44px">%s · %s</div>
              <div style="font-family:'Source Serif 4',serif;font-weight:700;font-size:78px;line-height:1;color:%s;margin-top:14px;letter-spacing:-1px">%s<span style="color:%s">.</span></div>
              <div style="font-family:Inter;font-size:27px;color:#6B6B6B;margin-top:12px">%s</div>
              <div style="margin-top:30px;position:relative;width:740px;height:940px;border-radius:24px;overflow:hidden;border:3px solid #E2DCD2">
                <img src="/s/p/%d/photo" style="width:100%%;height:100%%;object-fit:fill;display:block">
                <svg viewBox="0 0 1000 1000" preserveAspectRatio="none" style="position:absolute;inset:0;width:100%%;height:100%%">%s</svg>
                %s
              </div>
              <div style="margin-top:30px;display:flex;flex-direction:column;gap:16px">%s</div>
            </div>
            <div style="position:absolute;bottom:70px;left:90px;right:90px">
              <div style="font-family:'JetBrains Mono',monospace;font-weight:700;font-size:20px;letter-spacing:4px;color:#8A857B;text-align:center;margin-bottom:22px">VE LA GUÍA Y APÓRTALA TÚ · DESCARGA CUMBRE</div>
              %s
            </div>"""
            .formatted(LOGO, INK, TERRA, eyebrow, up(shortRegion(story.schoolName())), INK, title, TERRA,
                    author, story.postId(), paths, badges, list, BADGES);

        return page(PAPER, body);
    }

    /** Badge circular posicionado por % (alinea con la foto y no se deforma).
     *  Relleno con el color de la vía; borde y texto se adaptan (líneas claras
     *  → borde oscuro y texto oscuro; de color → borde blanco y texto blanco). */
    /** Texto tras el número gordo (= total de líneas): "bloques · 5 piedras",
     *  "vías", o "bloques y vías · 5 piedras" si mezcla (bloque != vía). */
    private static String noveltyBreakdown(NoveltyRow r) {
        String kind = r.boulders() > 0 && r.routes() > 0 ? "bloques y vías"
                : r.routes() > 0 ? (r.routes() == 1 ? "vía" : "vías")
                : (r.boulders() == 1 ? "bloque" : "bloques");
        if (r.blocks() > 0) {
            kind += " · " + r.blocks() + (r.blocks() == 1 ? " piedra" : " piedras");
        }
        return kind;
    }

    /** Clave canónica de un segmento (redondeo 4 decimales) = renderTopo app. */
    private static String segKey(double[] a, double[] b) {
        String ka = Math.round(a[0] * 10000) + "," + Math.round(a[1] * 10000);
        String kb = Math.round(b[0] * 10000) + "," + Math.round(b[1] * 10000);
        return ka.compareTo(kb) <= 0 ? ka + "|" + kb : kb + "|" + ka;
    }

    /** Desplazamiento X (0..1) de cada badge cuando varios coinciden (abanico). */
    private static java.util.List<Double> fanOffsets(java.util.List<double[]> anchors, double spacing) {
        java.util.Map<String, java.util.List<Integer>> groups = new java.util.HashMap<>();
        for (int i = 0; i < anchors.size(); i++) {
            double[] p = anchors.get(i);
            if (p != null) {
                groups.computeIfAbsent(Math.round(p[0] * 10000) + "," + Math.round(p[1] * 10000),
                        k -> new java.util.ArrayList<>()).add(i);
            }
        }
        Double[] out = new Double[anchors.size()];
        java.util.Arrays.fill(out, 0.0);
        for (var mem : groups.values()) {
            if (mem.size() > 1) {
                for (int j = 0; j < mem.size(); j++) {
                    out[mem.get(j)] = (j - (mem.size() - 1) / 2.0) * spacing;
                }
            }
        }
        return java.util.List.of(out);
    }

    /** Pinta una racha: discontinua normal, o FRANJA si la comparten 2+ vías. */
    private static void appendRun(StringBuilder paths, java.util.List<double[]> pts,
                                  String color, boolean dark,
                                  java.util.List<Integer> sharers, int lineIdx, double stripe) {
        if (pts.size() < 2) return;
        StringBuilder poly = new StringBuilder();
        for (double[] p : pts) {
            poly.append(Math.round(p[0] * 1000)).append(',').append(Math.round(p[1] * 1000)).append(' ');
        }
        String pl = poly.toString().trim();
        if (sharers.size() >= 2) {
            int n = sharers.size();
            int k = Math.max(0, sharers.indexOf(lineIdx));
            paths.append("<polyline points=\"").append(pl)
                 .append("\" fill=\"none\" stroke=\"").append(color)
                 .append("\" stroke-width=\"12\" stroke-linecap=\"butt\" stroke-linejoin=\"round\"")
                 .append(" stroke-dasharray=\"").append((int) stripe).append(' ').append((int) (stripe * (n - 1)))
                 .append("\" stroke-dashoffset=\"").append((int) (-k * stripe)).append("\"/>");
        } else {
            String dash = " stroke-dasharray=\"22 18\"";
            if (dark) {
                paths.append("<polyline points=\"").append(pl)
                     .append("\" fill=\"none\" stroke=\"#000\" stroke-opacity=\"0.55\" stroke-width=\"20\" stroke-linecap=\"round\" stroke-linejoin=\"round\"").append(dash).append("/>");
            }
            paths.append("<polyline points=\"").append(pl)
                 .append("\" fill=\"none\" stroke=\"").append(color)
                 .append("\" stroke-width=\"11\" stroke-linecap=\"round\" stroke-linejoin=\"round\"").append(dash).append("/>");
        }
    }

    private static String badge(double x, double y, String text, String color, boolean dark, int size) {
        String border = dark ? "#1A1A1A" : "#FFFFFF";
        String textColor = dark ? "#1A1A1A" : "#FFFFFF";
        return """
            <div style="position:absolute;left:%s%%;top:%s%%;transform:translate(-50%%,-50%%);width:%dpx;height:%dpx;border-radius:50%%;background:%s;border:5px solid %s;display:flex;align-items:center;justify-content:center;font-family:'Source Serif 4',serif;font-weight:700;font-size:%dpx;color:%s">%s</div>"""
            .formatted(fmt(x * 100), fmt(y * 100), size + 22, size + 22, color, border, size, textColor, esc(text));
    }

    private static String startLabel(String t) {
        if (t == null) return null;
        return switch (t.toUpperCase(Locale.ROOT)) {
            case "PIE", "STAND" -> "PIE";
            case "SIT" -> "SIT";
            case "LANCE", "JUMP" -> "LAN";
            case "TRAV" -> "TRV";
            default -> null;
        };
    }

    private static String fmt(double d) { return String.format(Locale.US, "%.2f", d); }

    // ───────────────────────────────────────────────────────── helpers

    /** Documento 1080x1920 autocontenido. */
    private static String page(String bg, String inner) {
        return """
            <!DOCTYPE html><html lang="es"><head><meta charset="utf-8">
            <style>
              @import url('https://fonts.googleapis.com/css2?family=Source+Serif+4:wght@400;600;700&family=Inter:wght@400;500;600&family=JetBrains+Mono:wght@500;700&display=swap');
              *{margin:0;padding:0;box-sizing:border-box}
              html,body{width:1080px;height:1920px}
              .s{width:1080px;height:1920px;position:relative;overflow:hidden;background:%s;font-family:Inter,sans-serif}
            </style></head><body><div class="s">%s</div></body></html>"""
            .formatted(bg, inner);
    }

    /** Fecha de hoy en español, "jueves 17 de julio". */
    private static String today() {
        LocalDate d = LocalDate.now();
        String dia = d.getDayOfWeek().getDisplayName(TextStyle.FULL, ES);
        String mes = d.getMonth().getDisplayName(TextStyle.FULL, ES);
        return dia + " " + d.getDayOfMonth() + " de " + mes;
    }

    /** Acorta el nombre de comunidad para el eyebrow ("Comunidad de Madrid"→"Madrid"). */
    static String shortRegion(String region) {
        if (region == null) return "";
        String r = region
                .replaceFirst("(?i)^comunidad (aut[oó]noma )?de (la )?", "")
                .replaceFirst("(?i)^comunidad ", "")
                .replaceFirst("(?i)^regi[oó]n de ", "")
                .replaceFirst("(?i)^islas ", "")
                .replaceFirst("(?i)^principado de ", "");
        return r.trim();
    }

    private static String up(String s) { return s == null ? "" : s.toUpperCase(ES); }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
