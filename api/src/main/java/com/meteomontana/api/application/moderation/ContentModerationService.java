package com.meteomontana.api.application.moderation;

import com.meteomontana.api.domain.port.NoteRepository;
import com.meteomontana.api.domain.port.PushSender;
import com.meteomontana.api.domain.port.UserRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.ContentReportJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.LineCommentJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataContentReportRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataLineCommentRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataUserBlockRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataUserRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.UserBlockJpaEntity;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    private static final Set<String> TARGET_TYPES = Set.of("COMMENT", "NOTE", "USER");
    private static final Set<String> REASONS = Set.of("SPAM", "OFFENSIVE", "FALSE_INFO", "OTHER");

    private final SpringDataContentReportRepository reports;
    private final SpringDataUserBlockRepository blocks;
    private final SpringDataLineCommentRepository comments;
    private final NoteRepository notes;
    private final UserRepository users;
    private final SpringDataUserRepository userJpa;
    private final PushSender push;

    public ContentModerationService(SpringDataContentReportRepository reports,
                                    SpringDataUserBlockRepository blocks,
                                    SpringDataLineCommentRepository comments,
                                    NoteRepository notes,
                                    UserRepository users,
                                    SpringDataUserRepository userJpa,
                                    PushSender push) {
        this.reports = reports;
        this.blocks = blocks;
        this.comments = comments;
        this.notes = notes;
        this.users = users;
        this.userJpa = userJpa;
        this.push = push;
    }

    /** Crea la denuncia (con snapshot del contenido) y avisa a los admins. */
    @Transactional
    public ReportView report(String reporterUid, String targetType, String targetId, String reason) {
        String type = targetType == null ? "" : targetType.trim().toUpperCase();
        if (!TARGET_TYPES.contains(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "targetType inválido");
        }
        String r = reason == null ? "OTHER" : reason.trim().toUpperCase();
        if (!REASONS.contains(r)) r = "OTHER";
        if (reports.existsByReporterUidAndTargetTypeAndTargetId(reporterUid, type, targetId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya lo has denunciado");
        }

        // Copia del contenido para que el admin lo juzgue aunque se borre.
        String snapshot = null;
        String authorUid = null;
        switch (type) {
            case "COMMENT" -> {
                LineCommentJpaEntity c = comments.findById(targetId).orElse(null);
                if (c != null) { snapshot = c.getAuthor() + ": " + c.getText(); authorUid = c.getUid(); }
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
        }

        ContentReportJpaEntity saved = reports.save(new ContentReportJpaEntity(
                UUID.randomUUID().toString(), reporterUid, type, targetId, r, snapshot, authorUid));
        notifyAdmins(type, snapshot);
        return toView(saved);
    }

    /** Push a todos los admins: hay una denuncia nueva que revisar. */
    @Async
    protected void notifyAdmins(String type, String snapshot) {
        String what = switch (type) {
            case "COMMENT" -> "un comentario";
            case "NOTE" -> "una nota";
            default -> "un usuario";
        };
        String body = snapshot == null ? "Toca para revisarla en el panel de admin"
                : snapshot.substring(0, Math.min(snapshot.length(), 100));
        userJpa.findByIsAdminTrue().forEach(admin ->
                push.sendToUser(admin.getUid(), "🚩 Denuncia nueva: " + what, body,
                        Map.of("targetType", "admin_reports", "targetId", "")));
    }

    @Transactional(readOnly = true)
    public List<ReportView> pending() {
        return reports.findByStatusOrderByCreatedAtDesc("PENDING").stream()
                .map(this::toView).toList();
    }

    /**
     * Resuelve una denuncia. action:
     *  - REMOVE  → borra el contenido denunciado (comentario/nota)
     *  - IGNORE  → la marca revisada sin tocar nada
     */
    @Transactional
    public ReportView resolve(String reportId, String action) {
        ContentReportJpaEntity rep = reports.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "denuncia no encontrada"));
        String a = action == null ? "IGNORE" : action.trim().toUpperCase();
        if ("REMOVE".equals(a)) {
            switch (rep.getTargetType()) {
                case "COMMENT" -> comments.findById(rep.getTargetId()).ifPresent(comments::delete);
                case "NOTE" -> notes.deleteById(rep.getTargetId());
                default -> { /* USER: no hay nada que borrar automáticamente */ }
            }
        }
        rep.resolve("REMOVE".equals(a) ? "REMOVED" : "IGNORED");
        return toView(reports.save(rep));
    }

    // ── Bloqueo entre usuarios ───────────────────────────────────────────

    @Transactional
    public void block(String blockerUid, String blockedUid) {
        if (blockerUid.equals(blockedUid)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "no puedes bloquearte a ti");
        }
        if (!blocks.existsByBlockerUidAndBlockedUid(blockerUid, blockedUid)) {
            blocks.save(new UserBlockJpaEntity(blockerUid, blockedUid));
        }
    }

    @Transactional
    public void unblock(String blockerUid, String blockedUid) {
        blocks.deleteById(new UserBlockJpaEntity.Key(blockerUid, blockedUid));
    }

    /** Uids bloqueados por este usuario (para filtrar su vista del contenido). */
    @Transactional(readOnly = true)
    public Set<String> blockedBy(String uid) {
        if (uid == null) return Set.of();
        return blocks.findByBlockerUid(uid).stream()
                .map(UserBlockJpaEntity::getBlockedUid)
                .collect(Collectors.toSet());
    }

    /** ¿Alguno de los dos tiene bloqueado al otro? (corta el chat en ambos sentidos). */
    @Transactional(readOnly = true)
    public boolean eitherBlocked(String a, String b) {
        return blocks.existsByBlockerUidAndBlockedUid(a, b)
                || blocks.existsByBlockerUidAndBlockedUid(b, a);
    }

    private ReportView toView(ContentReportJpaEntity e) {
        return new ReportView(e.getId(), e.getTargetType(), e.getTargetId(), e.getReason(),
                e.getSnapshot(), e.getAuthorUid(), e.getReporterUid(),
                e.getStatus(), e.getResolution(), e.getCreatedAt());
    }
}
