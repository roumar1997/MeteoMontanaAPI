package com.meteomontana.api.application.moderation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.meteomontana.api.domain.port.PushSender;
import com.meteomontana.api.infrastructure.persistence.jpa.ContentReportJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.MeetupReportJpaRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.ModerationActionJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataContentReportRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataModerationActionRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataUserRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.UserJpaEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Moderación de usuarios (consola de admin): aviso, suspensión temporal
 * (no puede crear contenido) y baneo de login (reversible). Además expone el
 * contador de denuncias recibidas por cada usuario para que el admin decida.
 */
@Service
public class UserModerationService {

    /** Resumen de moderación de un usuario para el panel de admin. */
    public record ReportRow(String type, String reason, String snapshot, String createdAt) {}
    /** Acción de moderación registrada (con motivo). */
    public record ActionRow(String action, String reason, String snapshot, String createdAt) {}
    public record ModerationView(String uid, String username, String displayName,
                                 boolean banned, LocalDateTime suspendedUntil, int warnings,
                                 long reportCount, List<ReportRow> reports, List<ActionRow> actions) {}

    private final SpringDataUserRepository users;
    private final SpringDataContentReportRepository contentReports;
    private final MeetupReportJpaRepository meetupReports;
    private final SpringDataModerationActionRepository actions;
    private final PushSender push;

    public UserModerationService(SpringDataUserRepository users,
                                 SpringDataContentReportRepository contentReports,
                                 MeetupReportJpaRepository meetupReports,
                                 SpringDataModerationActionRepository actions,
                                 PushSender push) {
        this.users = users;
        this.contentReports = contentReports;
        this.meetupReports = meetupReports;
        this.actions = actions;
        this.push = push;
    }

    @Transactional(readOnly = true)
    public ModerationView summary(String uid) {
        UserJpaEntity u = users.findById(uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "usuario no encontrado"));
        long count = contentReports.countByAuthorUid(uid) + meetupReports.countByReportedUid(uid);
        List<ReportRow> rows = contentReports.findByAuthorUidOrderByCreatedAtDesc(uid).stream()
                .map(this::toRow).toList();
        List<ActionRow> acts = actions.findByTargetUidOrderByCreatedAtDesc(uid).stream()
                .map(a -> new ActionRow(a.getAction(), a.getReason(), a.getSnapshot(),
                        a.getCreatedAt() == null ? null : a.getCreatedAt().toString()))
                .toList();
        return new ModerationView(u.getUid(), u.getUsername(), u.getDisplayName(),
                u.isBanned(), u.getSuspendedUntil(), u.getWarnings(), count, rows, acts);
    }

    /** Registra una acción de moderación (auditoría con motivo). */
    @Transactional
    public void record(String adminUid, String targetUid, String action, String reason, String snapshot) {
        actions.save(new ModerationActionJpaEntity(
                UUID.randomUUID().toString(), adminUid, targetUid, action, reason, snapshot));
    }

    /** Aviso: incrementa el contador y notifica al usuario por push. */
    @Transactional
    public void warn(String adminUid, String uid, String reason) {
        UserJpaEntity u = require(uid);
        u.setWarnings(u.getWarnings() + 1);
        users.save(u);
        record(adminUid, uid, "WARN", reason, null);
        String body = (reason == null || reason.isBlank())
                ? "Revisa las normas de la comunidad."
                : reason;
        push.sendToUser(uid, "⚠️ Aviso de moderación", body,
                Map.of("targetType", "warning", "targetId", ""));
    }

    /** Suspensión temporal: no podrá crear contenido hasta la fecha. */
    @Transactional
    public void suspend(String adminUid, String uid, int days, String reason) {
        if (uid.equals(adminUid)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes suspenderte a ti mismo.");
        }
        UserJpaEntity u = require(uid);
        int d = Math.max(1, days);
        u.setSuspendedUntil(LocalDateTime.now().plusDays(d));
        users.save(u);
        record(adminUid, uid, "SUSPEND", reason, "Suspendido " + d + " día(s)");
        push.sendToUser(uid, "⏸️ Cuenta suspendida",
                "No podrás publicar durante " + d + " día(s) por incumplir las normas.",
                Map.of("targetType", "warning", "targetId", ""));
    }

    /** Baneo de login (reversible). También lo deshabilita en Firebase Auth. */
    @Transactional
    public void ban(String adminUid, String uid, String reason) {
        if (uid.equals(adminUid)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes banearte a ti mismo.");
        }
        UserJpaEntity u = require(uid);
        u.setBanned(true);
        users.save(u);
        record(adminUid, uid, "BAN", reason, null);
        setFirebaseDisabled(uid, true);
    }

    @Transactional
    public void unban(String adminUid, String uid, String reason) {
        UserJpaEntity u = require(uid);
        u.setBanned(false);
        u.setSuspendedUntil(null);
        users.save(u);
        record(adminUid, uid, "UNBAN", reason, null);
        setFirebaseDisabled(uid, false);
    }

    /**
     * Cortafuegos de creación de contenido: lo llaman los casos de uso que
     * crean quedadas/notas/comentarios/fotos. Baneado o suspendido → 403.
     */
    @Transactional(readOnly = true)
    public void ensureCanPost(String uid) {
        if (uid == null) return;
        UserJpaEntity u = users.findById(uid).orElse(null);
        if (u == null) return;
        if (u.isBanned()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tu cuenta está suspendida.");
        }
        if (u.getSuspendedUntil() != null && u.getSuspendedUntil().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Estás suspendido hasta " + u.getSuspendedUntil().toLocalDate() + ".");
        }
    }

    private UserJpaEntity require(String uid) {
        return users.findById(uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "usuario no encontrado"));
    }

    private ReportRow toRow(ContentReportJpaEntity e) {
        return new ReportRow(e.getTargetType(), e.getReason(), e.getSnapshot(),
                e.getCreatedAt() == null ? null : e.getCreatedAt().toString());
    }

    /** Deshabilita/rehabilita en Firebase Auth (best-effort; el flag en BD manda). */
    private void setFirebaseDisabled(String uid, boolean disabled) {
        try {
            FirebaseAuth.getInstance().updateUser(
                    new UserRecord.UpdateRequest(uid).setDisabled(disabled));
        } catch (Exception ignored) { /* p.ej. usuario de prueba sin cuenta Firebase */ }
    }
}
