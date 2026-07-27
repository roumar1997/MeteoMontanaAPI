package com.meteomontana.api.application.moderation;

import com.meteomontana.api.domain.exception.BadRequestException;
import com.meteomontana.api.domain.exception.ConflictException;
import com.meteomontana.api.domain.exception.NotFoundException;
import com.meteomontana.api.domain.port.NoteRepository;
import com.meteomontana.api.domain.port.PushSender;
import com.meteomontana.api.domain.port.UserRepository;
import com.meteomontana.api.domain.model.ContentReport;
import com.meteomontana.api.domain.port.ContentReportRepository;
import com.meteomontana.api.domain.port.FeedCommentRepository;
import com.meteomontana.api.domain.port.FeedPostRepository;
import com.meteomontana.api.domain.port.LineCommentRepository;
import com.meteomontana.api.domain.port.UserBlockRepository;
import com.meteomontana.api.infrastructure.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Moderación de contenido de usuarios (requisito App Store para apps con UGC):
 * denuncias de comentarios/notas/usuarios con revisión de admin, y bloqueo
 * entre usuarios (el bloqueador deja de ver el contenido del bloqueado y este
 * no puede iniciarle chat).
 */
@Service
public class ContentModerationService {

    public record ReportView(String id, String targetType, String targetId, String reason,
                             String snapshot, String authorUid, String reporterUid,
                             String status, String resolution, LocalDateTime createdAt) {}

    private static final Set<String> TARGET_TYPES =
            Set.of("COMMENT", "NOTE", "USER", "FEED_POST", "FEED_COMMENT");
    private static final Set<String> REASONS = Set.of("SPAM", "OFFENSIVE", "FALSE_INFO", "OTHER");

    private final ContentReportRepository reports;
    private final UserBlockRepository blocks;
    private final LineCommentRepository comments;
    private final NoteRepository notes;
    private final UserRepository users;
    private final PushSender push;
    private final UserModerationService userModeration;
    private final FeedPostRepository feedPosts;
    private final FeedCommentRepository feedComments;
    private final StorageService storage;

    private static final Logger log = LoggerFactory.getLogger(ContentModerationService.class);

    public ContentModerationService(ContentReportRepository reports,
                                    UserBlockRepository blocks,
                                    LineCommentRepository comments,
                                    NoteRepository notes,
                                    UserRepository users,
                                    PushSender push,
                                    UserModerationService userModeration,
                                    FeedPostRepository feedPosts,
                                    FeedCommentRepository feedComments,
                                    StorageService storage) {
        this.reports = reports;
        this.blocks = blocks;
        this.comments = comments;
        this.notes = notes;
        this.users = users;
        this.push = push;
        this.userModeration = userModeration;
        this.feedPosts = feedPosts;
        this.feedComments = feedComments;
        this.storage = storage;
    }

    /** Crea la denuncia (con snapshot del contenido) y avisa a los admins. */
    @Transactional
    public ReportView report(String reporterUid, String targetType, String targetId, String reason) {
        String type = targetType == null ? "" : targetType.trim().toUpperCase();
        if (!TARGET_TYPES.contains(type)) {
            throw new BadRequestException("targetType inválido");
        }
        String r = reason == null ? "OTHER" : reason.trim().toUpperCase();
        if (!REASONS.contains(r)) r = "OTHER";
        if (reports.alreadyReported(reporterUid, type, targetId)) {
            throw new ConflictException("Ya lo has denunciado");
        }

        // Copia del contenido para que el admin lo juzgue aunque se borre.
        String snapshot = null;
        String authorUid = null;
        switch (type) {
            case "COMMENT" -> {
                var c = comments.findById(targetId).orElse(null);
                if (c != null) { snapshot = c.author() + ": " + c.text(); authorUid = c.uid(); }
            }
            case "NOTE" -> {
                var note = notes.findById(targetId).orElse(null);
                if (note != null) { snapshot = note.getAuthor() + ": " + note.getText(); authorUid = note.getUid(); }
            }
            case "USER" -> {
                var u = users.findByUid(targetId).orElse(null);
                if (u != null) {
                    snapshot = (u.getUsername() != null ? "@" + u.getUsername() : u.getDisplayName());
                    authorUid = targetId;
                }
            }
            case "FEED_POST" -> {
                var p = feedPostById(targetId);
                if (p != null) { snapshot = feedPostSnapshot(p); authorUid = p.getUserUid(); }
            }
            case "FEED_COMMENT" -> {
                var c = feedComments.findById(targetId).orElse(null);
                if (c != null) { snapshot = c.author() + ": " + c.text(); authorUid = c.uid(); }
            }
        }

        ContentReport saved = reports.create(new ContentReport(
                UUID.randomUUID().toString(), reporterUid, type, targetId, r, snapshot,
                authorUid, "PENDING", null, null, null));

        // Si quien denuncia es ADMIN, es moderación directa: el contenido se
        // retira YA (no va a la cola de revisión) y queda como auditoría.
        boolean isAdmin = users.findByUid(reporterUid)
                .map(u -> u.isAdmin()).orElse(false);
        if (isAdmin) {
            switch (type) {
                case "COMMENT" -> {
                    comments.deleteById(targetId);
                    userModeration.record(reporterUid, authorUid, "DELETE_COMMENT", r, snapshot);
                }
                case "NOTE" -> {
                    notes.deleteById(targetId);
                    userModeration.record(reporterUid, authorUid, "DELETE_NOTE", r, snapshot);
                }
                case "FEED_POST" -> {
                    var p = feedPostById(targetId);
                    if (p != null) {
                        feedPosts.deleteById(p.getId()); // likes/comentarios en cascada
                        deleteFeedPhotoQuietly(p.getPhotoPath());
                    }
                    userModeration.record(reporterUid, authorUid, "DELETE_FEED_POST", r, snapshot);
                }
                case "FEED_COMMENT" -> {
                    feedComments.deleteById(targetId);
                    userModeration.record(reporterUid, authorUid, "DELETE_FEED_COMMENT", r, snapshot);
                }
                default -> { /* USER: sin acción automática (el bloqueo ya es opción del diálogo) */ }
            }
            String resolution = "USER".equals(type) ? "IGNORED" : "REMOVED";
            reports.resolve(saved.id(), "RESOLVED", resolution, LocalDateTime.now());
            return toView(reports.findById(saved.id()).orElse(saved));
        }

        notifyAdmins(type, snapshot);
        return toView(saved);
    }

    /** Push a todos los admins: hay una denuncia nueva que revisar. */
    @Async
    protected void notifyAdmins(String type, String snapshot) {
        String what = switch (type) {
            case "COMMENT" -> "un comentario";
            case "NOTE" -> "una nota";
            case "FEED_POST" -> "un post del feed";
            case "FEED_COMMENT" -> "un comentario del feed";
            default -> "un usuario";
        };
        String body = snapshot == null ? "Toca para revisarla en el panel de admin"
                : snapshot.substring(0, Math.min(snapshot.length(), 100));
        users.findAdmins().forEach(admin ->
                push.sendToUser(admin.getUid(), "🚩 Denuncia nueva: " + what, body,
                        Map.of("targetType", "admin_reports", "targetId", "")));
    }

    @Transactional(readOnly = true)
    public List<ReportView> pending() {
        return reports.findByStatus("PENDING").stream()
                .map(this::toView).toList();
    }

    /**
     * Resuelve una denuncia. action:
     *  - REMOVE  → borra el contenido denunciado (comentario/nota)
     *  - IGNORE  → la marca revisada sin tocar nada
     */
    @Transactional
    public ReportView resolve(String adminUid, String reportId, String action, String reason) {
        ContentReport rep = reports.findById(reportId)
                .orElseThrow(() -> new NotFoundException("denuncia no encontrada"));
        String a = action == null ? "IGNORE" : action.trim().toUpperCase();
        if ("REMOVE".equals(a)) {
            // Motivo: el que escriba el admin o, en su defecto, el de la denuncia.
            String why = (reason == null || reason.isBlank()) ? rep.reason() : reason;
            switch (rep.targetType()) {
                case "COMMENT" -> {
                    comments.deleteById(rep.targetId());
                    userModeration.record(adminUid, rep.authorUid(), "DELETE_COMMENT", why, rep.snapshot());
                }
                case "NOTE" -> {
                    notes.deleteById(rep.targetId());
                    userModeration.record(adminUid, rep.authorUid(), "DELETE_NOTE", why, rep.snapshot());
                }
                case "FEED_POST" -> {
                    var p = feedPostById(rep.targetId());
                    if (p != null) {
                        feedPosts.deleteById(p.getId()); // likes/comentarios en cascada
                        deleteFeedPhotoQuietly(p.getPhotoPath());
                    }
                    userModeration.record(adminUid, rep.authorUid(), "DELETE_FEED_POST", why, rep.snapshot());
                }
                case "FEED_COMMENT" -> {
                    feedComments.deleteById(rep.targetId());
                    userModeration.record(adminUid, rep.authorUid(), "DELETE_FEED_COMMENT", why, rep.snapshot());
                }
                default -> { /* USER: no hay nada que borrar automáticamente */ }
            }
        }
        reports.resolve(reportId, "RESOLVED",
                "REMOVE".equals(a) ? "REMOVED" : "IGNORED", LocalDateTime.now());
        return toView(reports.findById(reportId).orElse(rep));
    }

    // ── Bloqueo entre usuarios ───────────────────────────────────────────

    @Transactional
    public void block(String blockerUid, String blockedUid) {
        if (blockerUid.equals(blockedUid)) {
            throw new BadRequestException("no puedes bloquearte a ti");
        }
        blocks.block(blockerUid, blockedUid);
    }

    @Transactional
    public void unblock(String blockerUid, String blockedUid) {
        blocks.unblock(blockerUid, blockedUid);
    }

    /** Uids bloqueados por este usuario (para filtrar su vista del contenido). */
    @Transactional(readOnly = true)
    public Set<String> blockedBy(String uid) {
        if (uid == null) return Set.of();
        return blocks.blockedUidsOf(uid);
    }

    /** ¿Alguno de los dos tiene bloqueado al otro? (corta el chat en ambos sentidos). */
    @Transactional(readOnly = true)
    public boolean eitherBlocked(String a, String b) {
        return blocks.isBlocked(a, b) || blocks.isBlocked(b, a);
    }

    /** El id de feed_posts es numérico; el targetId de la denuncia llega como String. */
    private com.meteomontana.api.domain.model.FeedPost feedPostById(String targetId) {
        try {
            return feedPosts.findById(Long.parseLong(targetId)).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Snapshot legible del post para que el admin lo juzgue aunque se borre. */
    private String feedPostSnapshot(com.meteomontana.api.domain.model.FeedPost p) {
        String author = users.findByUid(p.getUserUid())
                .map(u -> u.getUsername() != null ? "@" + u.getUsername() : u.getDisplayName())
                .orElse(p.getUserUid());
        String what = p.getLineName() != null ? p.getLineName() : p.getBlockName();
        // "(con foto)": aviso al admin de que el post tiene foto de celebración
        // que debe revisar (la ve en VER POST — photoUrl va en el FeedPostView).
        return author + ": " + p.getKind() + " «" + what + "»"
                + (p.getSchoolName() != null ? " · " + p.getSchoolName() : "")
                + (p.getPhotoPath() != null ? " (con foto)" : "");
    }

    /** Borra la foto de celebración del Storage, best effort: nunca tumba el borrado. */
    private void deleteFeedPhotoQuietly(String photoPath) {
        if (photoPath == null || photoPath.isBlank()) return;
        try {
            storage.delete(photoPath);
        } catch (Exception e) {
            log.warn("No se pudo borrar la foto del feed {}: {}", photoPath, e.getMessage());
        }
    }

    private ReportView toView(ContentReport e) {
        // FEED_COMMENT: al admin le sirve el POST padre (el botón VER POST abre
        // GET /api/feed/{id}); el snapshot ya enseña el texto del comentario.
        // La resolución sigue usando el id del reporte, así que borrar borra
        // el comentario, no el post.
        String targetId = e.targetId();
        if ("FEED_COMMENT".equals(e.targetType())) {
            var c = feedComments.findById(e.targetId()).orElse(null);
            if (c != null) targetId = String.valueOf(c.postId());
        }
        return new ReportView(e.id(), e.targetType(), targetId, e.reason(),
                e.snapshot(), e.authorUid(), e.reporterUid(),
                e.status(), e.resolution(), e.createdAt());
    }
}
