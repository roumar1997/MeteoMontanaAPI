package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.domain.model.BlockLine;
import com.meteomontana.api.domain.model.School;
import com.meteomontana.api.domain.port.SchoolRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SchoolBlockJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataSchoolBlockRepository;
import com.meteomontana.api.infrastructure.storage.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URI;
import java.util.Optional;

/**
 * Enlaces compartidos (WhatsApp etc.): páginas de aterrizaje con metadatos
 * Open Graph (tarjetita con foto en el chat) y App Links.
 *
 *  - GET /s/v/{lineId}   → landing de una vía/bloque (título, grado, foto)
 *  - GET /s/e/{schoolId} → landing de una escuela
 *  - GET /s/v/{lineId}/photo → 302 a una URL firmada fresca de la foto
 *
 * Si el receptor tiene la app, el móvil intercepta la URL (App Links /
 * Universal Links) y abre la vía directamente; si no, ve esta página con
 * los botones de descarga (Play / App Store según dispositivo).
 */
@RestController
public class ShareController {

    private static final String PLAY_URL =
            "https://play.google.com/store/apps/details?id=com.meteomontana.android";
    private static final String APPSTORE_URL = "https://apps.apple.com/app/id6785776686";

    private final SpringDataSchoolBlockRepository blocks;
    private final SchoolRepository schools;
    private final StorageService storage;
    private final com.meteomontana.api.domain.port.MeetupRepository meetups;
    private final com.meteomontana.api.infrastructure.persistence.jpa.SpringDataUserRepository users;
    private final com.meteomontana.api.infrastructure.persistence.jpa.SpringDataFeedPostRepository feedPosts;

    public ShareController(SpringDataSchoolBlockRepository blocks,
                           SchoolRepository schools,
                           StorageService storage,
                           com.meteomontana.api.domain.port.MeetupRepository meetups,
                           com.meteomontana.api.infrastructure.persistence.jpa.SpringDataUserRepository users,
                           com.meteomontana.api.infrastructure.persistence.jpa.SpringDataFeedPostRepository feedPosts) {
        this.blocks = blocks;
        this.schools = schools;
        this.storage = storage;
        this.meetups = meetups;
        this.users = users;
        this.feedPosts = feedPosts;
    }

    /**
     * Landing de una publicación del feed: /s/p/{postId}. Si el receptor tiene
     * la app, se abre el detalle del post; si no, tarjeta OG + descarga.
     * Solo posts de autores con perfil PÚBLICO (misma regla que el feed
     * Explorar): un post de perfil privado devuelve 404.
     */
    @GetMapping(value = "/s/p/{postId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> shareFeedPost(@PathVariable long postId) {
        var post = feedPosts.findById(postId).orElse(null);
        if (post == null) return ResponseEntity.notFound().build();
        var author = users.findById(post.getUserUid()).orElse(null);
        if (author == null || !author.isPublic()) return ResponseEntity.notFound().build();

        String who = author.getUsername() != null ? "@" + author.getUsername()
                : (author.getDisplayName() != null ? author.getDisplayName() : "Un escalador");
        String what = switch (post.getKind()) {
            case "NEW_BLOCK" -> "ha añadido la piedra «" + nz(post.getBlockName(), "nueva") + "»";
            case "NEW_LINE" -> "ha abierto «" + nz(post.getLineName(), "una vía") + "»"
                    + (post.getGrade() != null ? " " + post.getGrade() : "");
            case "PROJECT_DONE" -> "ha encadenado su proyecto «" + nz(post.getLineName(), "") + "»"
                    + (post.getGrade() != null ? " " + post.getGrade() : "");
            default -> "ha escalado «" + nz(post.getLineName(), "una vía") + "»"
                    + (post.getGrade() != null ? " " + post.getGrade() : "");
        };
        String title = who + " " + what + " · Cumbre";
        String desc = (post.getSchoolName() != null ? "En " + post.getSchoolName() + ". " : "")
                + "Mira la publicación con la línea dibujada en Cumbre.";
        // Imagen: foto de celebración del post o, si no, la cara de la piedra.
        boolean hasImage = post.getPhotoPath() != null || blockCoverPhoto(post.getBlockId()) != null;
        String img = hasImage ? "/s/p/" + postId + "/photo" : null;
        return ResponseEntity.ok(landing(title, desc, "/s/p/" + postId, img));
    }

    @GetMapping(value = "/s/p/{postId}/photo")
    public RedirectView shareFeedPostPhoto(@PathVariable long postId) {
        var post = feedPosts.findById(postId).orElse(null);
        String photo = null;
        if (post != null) {
            var author = users.findById(post.getUserUid()).orElse(null);
            if (author != null && author.isPublic()) {
                photo = post.getPhotoPath() != null ? post.getPhotoPath()
                        : blockCoverPhoto(post.getBlockId());
            }
        }
        if (photo == null) {
            RedirectView rv = new RedirectView("/");
            rv.setStatusCode(HttpStatus.NOT_FOUND);
            return rv;
        }
        return new RedirectView(storage.signedReadUrl(photo, 10).toString());
    }

    private String blockCoverPhoto(String blockId) {
        if (blockId == null) return null;
        return blocks.findById(blockId).map(SchoolBlockJpaEntity::getPhotoPath).orElse(null);
    }

    private static String nz(String s, String fallback) {
        return s == null || s.isBlank() ? fallback : s;
    }

    /** Landing de un perfil: /s/u/{username o uid}. La app lo abre directo. */
    @GetMapping(value = "/s/u/{handle}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> shareUser(@PathVariable String handle) {
        String h = handle.startsWith("@") ? handle.substring(1) : handle;
        var user = users.findByUsernameIgnoreCase(h).or(() -> users.findById(h)).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        String name = user.getUsername() != null ? "@" + user.getUsername()
                : (user.getDisplayName() != null ? user.getDisplayName() : "Un escalador");
        String title = name + " · Cumbre";
        // Solo datos ya públicos en la app; si el perfil es privado, texto genérico.
        String desc = user.isPublic() && user.getBio() != null && !user.getBio().isBlank()
                ? user.getBio()
                : "Mira su perfil, sus bloques y sus vías en Cumbre.";
        return ResponseEntity.ok(landing(title, desc,
                "/s/u/" + (user.getUsername() != null ? user.getUsername() : user.getUid()), null));
    }

    @GetMapping(value = "/s/v/{schoolId}/{lineId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> shareLine(@PathVariable String schoolId, @PathVariable String lineId) {
        SchoolBlockJpaEntity block = blocks.findByLineId(lineId).orElse(null);
        if (block == null) return ResponseEntity.notFound().build();
        var line = block.getLines().stream()
                .filter(l -> lineId.equals(l.getId())).findFirst().orElse(null);
        if (line == null) return ResponseEntity.notFound().build();

        String schoolName = schools.findById(block.getSchoolId())
                .map(School::getName).orElse(block.getSchoolId());
        boolean isBoulder = block.getDiscipline()
                != com.meteomontana.api.domain.model.SchoolBlock.Discipline.ROUTE;
        String kind = isBoulder ? "bloque" : "vía";
        String grade = line.getGrade() == null ? "" : " " + line.getGrade();
        String title = "«" + line.getName() + "»" + grade + " · " + block.getName() + " · Cumbre";
        String desc = (isBoulder ? "Bloque" : "Vía") + " en " + schoolName
                + ". Toca para verla con la línea dibujada en Cumbre.";
        String photo = line.getPhotoPath() != null ? line.getPhotoPath() : block.getPhotoPath();
        String img = photo != null ? "/s/v/" + schoolId + "/" + lineId + "/photo" : null;
        return ResponseEntity.ok(landing(title, desc, "/s/v/" + schoolId + "/" + lineId, img));
    }

    @GetMapping(value = "/s/v/{schoolId}/{lineId}/photo")
    public RedirectView sharePhoto(@PathVariable String schoolId, @PathVariable String lineId) {
        SchoolBlockJpaEntity block = blocks.findByLineId(lineId).orElse(null);
        String photo = null;
        if (block != null) {
            photo = block.getLines().stream()
                    .filter(l -> lineId.equals(l.getId())).findFirst()
                    .map(l -> l.getPhotoPath() != null ? l.getPhotoPath() : block.getPhotoPath())
                    .orElse(block.getPhotoPath());
        }
        if (photo == null) {
            RedirectView rv = new RedirectView("/");
            rv.setStatusCode(HttpStatus.NOT_FOUND);
            return rv;
        }
        // URL firmada fresca en cada petición: las de Firebase caducan.
        return new RedirectView(storage.signedReadUrl(photo, 10).toString());
    }

    @GetMapping(value = "/s/e/{schoolId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> shareSchool(@PathVariable String schoolId) {
        Optional<School> school = schools.findById(schoolId);
        if (school.isEmpty()) return ResponseEntity.notFound().build();
        School s = school.get();
        String title = s.getName() + " · Cumbre";
        String desc = "Escuela de escalada"
                + (s.getRegion() != null ? " en " + s.getRegion() : "")
                + ". Índice de escalabilidad, mapa de piedras y previsión en Cumbre.";
        return ResponseEntity.ok(landing(title, desc, "/s/e/" + schoolId, null));
    }

    /**
     * Enlace único de descarga para QR físicos (pegatinas): URL FIJA y
     * permanente. Redirige por dispositivo: iPhone/iPad → App Store,
     * Android → Play; ordenador u otros → página con los dos botones.
     * NO cambiar la ruta /app: hay QR impresos apuntando aquí.
     */
    @GetMapping(value = "/app")
    public Object downloadApp(
            @org.springframework.web.bind.annotation.RequestHeader(value = "User-Agent", required = false)
            String userAgent) {
        String ua = userAgent == null ? "" : userAgent;
        if (ua.matches("(?i).*(iphone|ipad|ipod).*")) return new RedirectView(APPSTORE_URL);
        if (ua.matches("(?i).*android.*")) return new RedirectView(PLAY_URL);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body("""
                <!DOCTYPE html><html lang="es"><head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Descarga Cumbre</title>
                <meta property="og:title" content="Cumbre · Escalada">
                <meta property="og:description" content="Escuelas, piedras, previsión y comunidad de escalada. Descarga la app.">
                <meta property="og:site_name" content="Cumbre">
                <style>
                  body{font-family:-apple-system,Roboto,sans-serif;background:#F5F3EE;color:#1C1C1A;
                       margin:0;display:flex;min-height:100vh;align-items:center;justify-content:center}
                  .card{max-width:420px;padding:32px 24px;text-align:center}
                  h1{font-family:Georgia,serif;font-size:26px;margin:16px 0 6px}
                  p{color:#5A574F;font-size:15px;line-height:1.5}
                  a.btn{display:block;background:#C2410C;color:#fff;text-decoration:none;
                        border-radius:10px;padding:14px;margin:10px 0;font-weight:600}
                  .eyebrow{font-family:monospace;font-size:11px;letter-spacing:2px;color:#C2410C}
                </style></head><body><div class="card">
                <div class="eyebrow">CUMBRE · ESCALADA</div>
                <h1>Descarga Cumbre</h1>
                <p>Escuelas, piedras, previsión por horas y comunidad de escalada.</p>
                <a class="btn" href="%PLAY%">Google Play (Android)</a>
                <a class="btn" href="%APPSTORE%">App Store (iPhone y iPad)</a>
                </div></body></html>
                """.replace("%PLAY%", PLAY_URL).replace("%APPSTORE%", APPSTORE_URL));
    }

    /** Página mínima: metadatos OG + botón de la store correcta por dispositivo. */
    private String landing(String title, String desc, String path, String imagePath) {
        String base = "https://api.climbingteams.com";
        String url = base + path;
        String img = imagePath != null ? base + imagePath : "";
        String ogImage = imagePath != null
                ? "<meta property=\"og:image\" content=\"" + img + "\">" : "";
        return """
                <!DOCTYPE html><html lang="es"><head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>%TITLE%</title>
                <meta property="og:title" content="%TITLE%">
                <meta property="og:description" content="%DESC%">
                <meta property="og:url" content="%URL%">
                <meta property="og:site_name" content="Cumbre">
                %OGIMG%
                <style>
                  body{font-family:-apple-system,Roboto,sans-serif;background:#F5F3EE;color:#1C1C1A;
                       margin:0;display:flex;min-height:100vh;align-items:center;justify-content:center}
                  .card{max-width:420px;padding:32px 24px;text-align:center}
                  img{max-width:100%;border-radius:12px;border:1px solid #D6D2C4}
                  h1{font-family:Georgia,serif;font-size:26px;margin:16px 0 6px}
                  p{color:#5A574F;font-size:15px;line-height:1.5}
                  a.btn{display:block;background:#C2410C;color:#fff;text-decoration:none;
                        border-radius:10px;padding:14px;margin:10px 0;font-weight:600}
                  .eyebrow{font-family:monospace;font-size:11px;letter-spacing:2px;color:#C2410C}
                </style></head><body><div class="card">
                <div class="eyebrow">CUMBRE · ESCALADA</div>
                %IMGTAG%
                <h1>%TITLE_PLAIN%</h1>
                <p>%DESC%</p>
                <p>Si ya tienes Cumbre, este enlace se abre directamente en la app.</p>
                <a class="btn" id="store" href="%PLAY%">Descargar Cumbre</a>
                <script>
                  if (/iPhone|iPad|iPod/.test(navigator.userAgent)) {
                    document.getElementById('store').href = '%APPSTORE%';
                  }
                </script>
                </div></body></html>
                """
                .replace("%TITLE_PLAIN%", esc(title.replace(" · Cumbre", "")))
                .replace("%TITLE%", esc(title))
                .replace("%DESC%", esc(desc))
                .replace("%URL%", url)
                .replace("%OGIMG%", ogImage)
                .replace("%IMGTAG%", imagePath != null ? "<img src=\"" + img + "\" alt=\"\">" : "")
                .replace("%PLAY%", PLAY_URL)
                .replace("%APPSTORE%", APPSTORE_URL);
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace("\"", "&quot;");
    }

    /** Invitación a una quedada: /s/q/{id}?i={token}. La app la abre directa. */
    @GetMapping(value = "/s/q/{meetupId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> shareMeetup(@PathVariable String meetupId) {
        var meetup = meetups.findById(meetupId).orElse(null);
        if (meetup == null) return ResponseEntity.notFound().build();
        String schoolName = schools.findById(meetup.getSchoolId())
                .map(School::getName).orElse(meetup.getSchoolId());
        String title = "Quedada: " + meetup.getName() + " · Cumbre";
        String days = meetup.getDays() == null ? "" : meetup.getDays().stream()
                .map(Object::toString).reduce((a, b) -> a + ", " + b).orElse("");
        String desc = "Te han invitado a escalar en " + schoolName
                + (days.isEmpty() ? "" : " (" + days + ")")
                + ". Toca para unirte desde Cumbre.";
        return ResponseEntity.ok(landing(title, desc, "/s/q/" + meetupId, null));
    }

    // ── Ficheros de verificación de App Links ──────────────────────────────

    /** Android App Links. OJO: si Play App Signing re-firma, añadir TAMBIÉN la
     *  huella del certificado de Play (Play Console → Integridad de la app). */
    @GetMapping(value = "/.well-known/assetlinks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public String assetLinks() {
        return """
                [{
                  "relation": ["delegate_permission/common.handle_all_urls"],
                  "target": {
                    "namespace": "android_app",
                    "package_name": "com.meteomontana.android",
                    "sha256_cert_fingerprints": [
                      "ED:96:47:8A:9D:56:1B:38:50:87:36:9C:D1:84:3B:73:06:1D:0E:58:6E:B0:A8:9A:3B:E2:65:32:D6:CA:07:85",
                      "4B:50:EB:61:CA:5D:22:DE:78:36:0A:EC:47:F9:A7:74:71:6A:B9:48:D8:36:79:DC:E0:A5:43:97:67:B5:D4:BF"
                    ]
                  }
                }]
                """;
    }

    /** iOS Universal Links (Team 3CP2YRJ579). */
    @GetMapping(value = "/.well-known/apple-app-site-association", produces = MediaType.APPLICATION_JSON_VALUE)
    public String appleAppSiteAssociation() {
        return """
                {
                  "applinks": {
                    "details": [{
                      "appIDs": ["3CP2YRJ579.com.meteomontana.ios.3CP2YRJ579"],
                      "components": [{ "/": "/s/*" }]
                    }]
                  }
                }
                """;
    }
}
