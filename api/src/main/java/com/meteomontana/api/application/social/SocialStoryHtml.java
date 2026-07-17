package com.meteomontana.api.application.social;

import com.meteomontana.api.application.social.SocialStoryService.ConditionRow;
import com.meteomontana.api.application.social.SocialStoryService.ConditionsStory;

import java.time.LocalDate;
import java.time.format.TextStyle;
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

    /** Anillos de radar (esquina superior derecha). */
    private static final String RADAR = """
        <svg style="position:absolute;top:-260px;right:-260px;width:900px;height:900px" viewBox="0 0 900 900">
          <g fill="none" stroke="#C0532B">
            <circle cx="450" cy="450" r="150" stroke-opacity="0.13" stroke-width="3"/>
            <circle cx="450" cy="450" r="255" stroke-opacity="0.11" stroke-width="3"/>
            <circle cx="450" cy="450" r="355" stroke-opacity="0.09" stroke-width="3"/>
            <circle cx="450" cy="450" r="450" stroke-opacity="0.07" stroke-width="3"/></g>
        </svg>""";

    // ─────────────────────────────────────────────── condiciones diarias

    public static String conditions(ConditionsStory story) {
        StringBuilder rows = new StringBuilder();
        int i = 1;
        for (ConditionRow s : story.schools()) {
            String col = s.score() >= 70 ? GREEN : (s.score() >= 45 ? TERRA : "#B23B3B");
            String dot = s.dryRock() ? GREEN : "#B23B3B";
            rows.append("""
                <div style="display:flex;align-items:center;background:#FAF8F3;border:3px solid #E2DCD2;border-radius:26px;padding:26px 34px">
                  <div style="font-family:'Source Serif 4',serif;font-weight:700;font-size:52px;color:%s;width:74px">%02d</div>
                  <div style="flex:1">
                    <div style="font-family:'Source Serif 4',serif;font-weight:600;font-size:44px;color:%s;line-height:1.05">%s</div>
                    <div style="font-family:Inter;font-size:25px;color:#6B6B6B;margin-top:5px"><span style="display:inline-block;width:14px;height:14px;border-radius:50%%;background:%s;margin-right:9px"></span>Roca %s · %d° · viento %d km/h</div>
                  </div>
                  <div style="text-align:right"><span style="font-family:'Source Serif 4',serif;font-weight:700;font-size:70px;color:%s">%d</span><span style="font-family:'JetBrains Mono',monospace;font-size:26px;color:#8A857B">/100</span></div>
                </div>"""
                .formatted(TERRA, i, INK, esc(s.name()), dot,
                        s.dryRock() ? "seca" : "húmeda", s.temp(), s.wind(), col, s.score()));
            i++;
        }

        String body = """
            <div style="padding:96px 90px 0;position:relative">
              <div style="display:flex;align-items:center;gap:22px">%s
                <div><div style="font-family:'Source Serif 4',serif;font-weight:700;font-size:40px;color:%s;line-height:1">Cumbre</div>
                <div style="font-family:'JetBrains Mono',monospace;font-weight:500;font-size:19px;letter-spacing:5px;color:#8A857B">METEO PARA ESCALAR</div></div>
              </div>
              <div style="font-family:'JetBrains Mono',monospace;font-weight:700;font-size:26px;letter-spacing:7px;color:%s;margin-top:110px">CONDICIONES DE HOY · %s</div>
              <div style="font-family:'Source Serif 4',serif;font-weight:700;font-size:100px;line-height:1;color:%s;margin-top:22px;letter-spacing:-2px">¿Dónde escalar hoy?</div>
              <div style="font-family:Inter;font-size:30px;color:#6B6B6B;margin-top:22px">%s</div>
              <div style="margin-top:56px;display:flex;flex-direction:column;gap:22px">%s</div>
            </div>
            <div style="position:absolute;bottom:96px;left:90px;right:90px">
              <div style="font-family:'JetBrains Mono',monospace;font-weight:700;font-size:22px;letter-spacing:5px;color:#8A857B;text-align:center;margin-bottom:26px">DESCÁRGALA GRATIS</div>
              %s
            </div>"""
            .formatted(LOGO, INK, TERRA, up(shortRegion(story.region())), INK, today(), rows, BADGES);

        return page(PAPER, RADAR + body);
    }

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
