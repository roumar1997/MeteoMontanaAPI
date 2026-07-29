package com.meteomontana.api.application.moderation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.meteomontana.api.domain.exception.BadRequestException;
import com.meteomontana.api.domain.exception.ForbiddenException;
import com.meteomontana.api.domain.exception.NotFoundException;
import com.meteomontana.api.domain.port.PushSender;
import com.meteomontana.api.domain.model.ContentReport;
import com.meteomontana.api.domain.model.UserModerationState;
import com.meteomontana.api.domain.port.ContentReportRepository;
import com.meteomontana.api.domain.port.UserModerationRepository;
import com.meteomontana.api.domain.port.MeetupReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * Moderación de usuarios (consola de admin): aviso, suspensión temporal
 * (no puede crear contenido) y baneo de login (reversible). Además expone el
 * contador de denuncias recibidas por cada usuario para que el admin decida.
 */
@Service
@RequiredArgsConstructor
public class UserModerationService {

    /** Resumen de moderación de un usuario para el panel de admin. */
    public record ReportRow(String type, String reason, String snapshot, String createdAt) {}
    /** Acción de moderación registrada (con motivo). */
    public record ActionRow(String action, String reason, String snapshot, String createdAt) {}
    public record ModerationView(String uid, String username, String displayName,
                                 boolean banned, LocalDateTime suspendedUntil, int warnings,
                                 long reportCount, List<ReportRow> reports, List<ActionRow> actions) {}

    private final UserModerationRepository moderation;
    private final ContentReportRepository contentReports;
    private final MeetupReportRepository meetupReports;
    private final PushSender push;

    /**
     * 5.2 — Caché del cortafuegos de publicación. `ensureCanPost` lo llaman
     * TODOS los endpoints de creación (nota, comentario, quedada, foto, post):
     * sin caché es una consulta a BD por cada publicación. TTL corto (30 s) y
     * se invalida al momento en warn/suspend/ban/unban, así que una sanción
     * surte efecto inmediato y aun así se ahorran las consultas repetidas.
     */
    private final com.github.benmanes.caffeine.cache.Cache<String, UserModerationState> stateCache =
            com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                    .expireAfterWrite(java.time.Duration.ofSeconds(30))
                    .maximumSize(10_000)
                    .build();

    @Transactional(readOnly = true)
    public ModerationView summary(String uid) {
        UserModerationState st = moderation.findState(uid)
                .orElseThrow(() -> new NotFoundException("usuario no encontrado"));
        long count = contentReports.countByAuthor(uid) + meetupReports.countByReportedUid(uid);
        List<ReportRow> rows = contentReports.findByAuthor(uid).stream()
                .map(this::toRow).toList();
        List<ActionRow> acts = moderation.actionsOf(uid).stream()
                .map(a -> new ActionRow(a.action(), a.reason(), a.snapshot(),
                        a.createdAt() == null ? null : a.createdAt().toString()))
                .toList();
        return new ModerationView(st.uid(), st.username(), st.displayName(),
                st.banned(), st.suspendedUntil(), st.warnings(), count, rows, acts);
    }

    /** Registra una acción de moderación (auditoría con motivo). */
    @Transactional
    public void record(String adminUid, String targetUid, String action, String reason, String snapshot) {
        moderation.recordAction(adminUid, targetUid, action, reason, snapshot);
    }

    /** Aviso: incrementa el contador y notifica al usuario por push. */
    @Transactional
    public void warn(String adminUid, String uid, String reason) {
        moderation.addWarning(uid);
        stateCache.invalidate(uid);
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
            throw new BadRequestException("No puedes suspenderte a ti mismo.");
        }
        int d = Math.max(1, days);
        moderation.setSuspendedUntil(uid, LocalDateTime.now().plusDays(d));
        stateCache.invalidate(uid);
        record(adminUid, uid, "SUSPEND", reason, "Suspendido " + d + " día(s)");
        push.sendToUser(uid, "⏸️ Cuenta suspendida",
                "No podrás publicar durante " + d + " día(s) por incumplir las normas.",
                Map.of("targetType", "warning", "targetId", ""));
    }

    /** Baneo de login (reversible). También lo deshabilita en Firebase Auth. */
    @Transactional
    public void ban(String adminUid, String uid, String reason) {
        if (uid.equals(adminUid)) {
            throw new BadRequestException("No puedes banearte a ti mismo.");
        }
        moderation.setBanned(uid, true);
        stateCache.invalidate(uid);
        record(adminUid, uid, "BAN", reason, null);
        setFirebaseDisabled(uid, true);
    }

    @Transactional
    public void unban(String adminUid, String uid, String reason) {
        moderation.setBanned(uid, false);   // desbanear limpia la suspensión
        stateCache.invalidate(uid);
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
        UserModerationState st = stateCache.get(uid,
                k -> moderation.findState(k).orElse(null));
        if (st == null) return;
        if (st.banned()) {
            throw new ForbiddenException("Tu cuenta está suspendida.");
        }
        if (st.suspendedUntil() != null && st.suspendedUntil().isAfter(LocalDateTime.now())) {
            throw new ForbiddenException(
                    "Estás suspendido hasta " + st.suspendedUntil().toLocalDate() + ".");
        }
    }

    private ReportRow toRow(ContentReport r) {
        return new ReportRow(r.targetType(), r.reason(), r.snapshot(),
                r.createdAt() == null ? null : r.createdAt().toString());
    }

    /** Deshabilita/rehabilita en Firebase Auth (best-effort; el flag en BD manda). */
    private void setFirebaseDisabled(String uid, boolean disabled) {
        try {
            FirebaseAuth.getInstance().updateUser(
                    new UserRecord.UpdateRequest(uid).setDisabled(disabled));
        } catch (Exception ignored) { /* p.ej. usuario de prueba sin cuenta Firebase */ }
    }
}
