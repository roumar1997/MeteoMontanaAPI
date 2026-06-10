package com.meteomontana.api.infrastructure.email;

/**
 * Plantilla HTML de marca para los emails transaccionales de Cumbre.
 * Estilo "Cumbre": papel, tinta, terracota — espejo del design system de la app.
 * Todo inline-style (los clientes de correo ignoran <style> en gran parte).
 */
public final class EmailTemplates {

    private EmailTemplates() {}

    private static final String PAPER  = "#f2eee3";
    private static final String CARD   = "#fbf9f4";
    private static final String INK    = "#1c1c1a";
    private static final String DIM    = "#6b675e";
    private static final String TERRA  = "#c2410c";
    private static final String RULE   = "#d8d2c2";
    private static final String LOGO   = "https://climbingteams.com/icons/icon-192.png";

    /**
     * Envuelve el contenido en el layout de marca.
     *
     * @param preheader texto corto que los clientes muestran junto al asunto
     * @param innerHtml bloques HTML del cuerpo (usar los helpers de abajo)
     */
    public static String wrap(String preheader, String innerHtml) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <body style="margin:0;padding:0;background:%s;">
              <div style="display:none;max-height:0;overflow:hidden;">%s</div>
              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:%s;">
                <tr><td align="center" style="padding:32px 16px;">
                  <table role="presentation" width="560" cellpadding="0" cellspacing="0" style="max-width:560px;width:100%%;">

                    <!-- Header -->
                    <tr><td align="center" style="padding-bottom:20px;">
                      <img src="%s" width="56" height="56" alt="Cumbre"
                           style="border-radius:50%%;display:block;margin:0 auto 10px;">
                      <div style="font-family:'Courier New',monospace;font-size:11px;letter-spacing:3px;
                                  color:%s;text-transform:uppercase;">&#9670; CUMBRE</div>
                    </td></tr>

                    <!-- Card -->
                    <tr><td style="background:%s;border:1px solid %s;border-radius:4px;padding:32px 28px;
                                   font-family:Georgia,'Times New Roman',serif;color:%s;">
                      %s
                    </td></tr>

                    <!-- Footer -->
                    <tr><td align="center" style="padding-top:24px;">
                      <div style="font-family:'Courier New',monospace;font-size:10px;letter-spacing:1.5px;
                                  color:%s;text-transform:uppercase;line-height:1.8;">
                        CUMBRE &middot; METEO PARA ESCALADORES<br>
                        <a href="https://climbingteams.com" style="color:%s;text-decoration:none;">climbingteams.com</a>
                        &nbsp;&middot;&nbsp;
                        <a href="https://climbingteams.com/privacy.html" style="color:%s;text-decoration:none;">privacidad</a>
                      </div>
                    </td></tr>

                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(PAPER, escape(preheader), PAPER, LOGO, TERRA,
                          CARD, RULE, INK, innerHtml, DIM, DIM, DIM);
    }

    /** Eyebrow mono en terracota, p.ej. "PROPUESTA APROBADA". */
    public static String eyebrow(String text) {
        return "<div style=\"font-family:'Courier New',monospace;font-size:11px;letter-spacing:2px;"
                + "color:" + TERRA + ";text-transform:uppercase;margin-bottom:10px;\">"
                + escape(text) + "</div>";
    }

    /** Titular serif grande. */
    public static String title(String text) {
        return "<h1 style=\"margin:0 0 16px;font-size:24px;font-weight:600;letter-spacing:-0.3px;"
                + "color:" + INK + ";\">" + escape(text) + "</h1>";
    }

    /** Párrafo normal. `html` permite negritas/cursivas ya escapadas por el caller. */
    public static String paragraph(String html) {
        return "<p style=\"margin:0 0 14px;font-size:15px;line-height:1.7;color:" + INK + ";\">"
                + html + "</p>";
    }

    /** Caja destacada con borde terra (motivo de rechazo, detalle de la propuesta...). */
    public static String highlightBox(String label, String html) {
        return "<div style=\"border-left:3px solid " + TERRA + ";background:" + PAPER + ";"
                + "padding:12px 16px;margin:0 0 14px;\">"
                + "<div style=\"font-family:'Courier New',monospace;font-size:10px;letter-spacing:1.5px;"
                + "color:" + DIM + ";text-transform:uppercase;margin-bottom:4px;\">" + escape(label) + "</div>"
                + "<div style=\"font-size:14px;line-height:1.6;color:" + INK + ";\">" + html + "</div>"
                + "</div>";
    }

    /** Botón primario terra. */
    public static String button(String label, String url) {
        return "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:20px 0 4px;\">"
                + "<tr><td style=\"background:" + TERRA + ";border-radius:2px;\">"
                + "<a href=\"" + url + "\" style=\"display:inline-block;padding:12px 24px;"
                + "font-family:'Courier New',monospace;font-size:12px;letter-spacing:2px;"
                + "color:#ffffff;text-decoration:none;text-transform:uppercase;\">"
                + escape(label) + "</a></td></tr></table>";
    }

    /** Firma estándar. */
    public static String signature() {
        return "<p style=\"margin:18px 0 0;font-size:14px;color:" + DIM + ";\">"
                + "&mdash; El equipo de Cumbre</p>";
    }

    public static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
