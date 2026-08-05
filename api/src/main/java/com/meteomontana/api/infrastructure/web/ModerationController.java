package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.admin.AdminGuard;
import com.meteomontana.api.application.moderation.ContentModerationService;
import com.meteomontana.api.application.users.UserIdentifierResolver;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/** Denuncias de contenido + bloqueo entre usuarios (moderación UGC). */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ModerationController {

    public record ReportRequest(String targetType, String targetId, String reason) {}
    public record ResolveRequest(String action, String reason) {}

    private final ContentModerationService service;
    private final AdminGuard adminGuard;
    /** El perfil abierto desde una mención pasa el username, no el uid. */
    private final UserIdentifierResolver resolver;

    @PostMapping("/reports")
    public ContentModerationService.ReportView report(
            @AuthenticationPrincipal FirebaseUser user,
            @RequestBody ReportRequest req) {
        return service.report(user.uid(), req.targetType(), req.targetId(), req.reason());
    }

    // ── Bloqueo ──────────────────────────────────────────────────────────

    @PostMapping("/users/{uid}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void block(@AuthenticationPrincipal FirebaseUser user, @PathVariable String uid) {
        service.block(user.uid(), resolver.requireUid(uid));
    }

    @DeleteMapping("/users/{uid}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unblock(@AuthenticationPrincipal FirebaseUser user, @PathVariable String uid) {
        service.unblock(user.uid(), resolver.requireUid(uid));
    }

    @GetMapping("/me/blocked")
    public Set<String> blocked(@AuthenticationPrincipal FirebaseUser user) {
        return service.blockedBy(user.uid());
    }

    // ── Admin ────────────────────────────────────────────────────────────

    @GetMapping("/admin/content-reports")
    public List<ContentModerationService.ReportView> pending(
            @AuthenticationPrincipal FirebaseUser user) {
        adminGuard.ensureAdmin(user.uid());
        return service.pending();
    }

    @PostMapping("/admin/content-reports/{id}/resolve")
    public ContentModerationService.ReportView resolve(
            @AuthenticationPrincipal FirebaseUser user,
            @PathVariable String id,
            @RequestBody ResolveRequest req) {
        adminGuard.ensureAdmin(user.uid());
        return service.resolve(user.uid(), id, req.action(), req.reason());
    }
}
