package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.admin.AdminGuard;
import com.meteomontana.api.application.moderation.UserModerationService;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Consola de moderación de usuarios (solo admin): ver el historial de denuncias
 * de un usuario y aplicar consecuencias — aviso, suspensión temporal, baneo de
 * login (reversible).
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminModerationController {

    private final UserModerationService moderation;
    private final AdminGuard adminGuard;

    public AdminModerationController(UserModerationService moderation, AdminGuard adminGuard) {
        this.moderation = moderation;
        this.adminGuard = adminGuard;
    }

    @GetMapping("/{uid}/moderation")
    public UserModerationService.ModerationView summary(
            @AuthenticationPrincipal FirebaseUser user, @PathVariable String uid) {
        adminGuard.ensureAdmin(user.uid());
        return moderation.summary(uid);
    }

    @PostMapping("/{uid}/warn")
    public UserModerationService.ModerationView warn(
            @AuthenticationPrincipal FirebaseUser user, @PathVariable String uid,
            @RequestBody(required = false) Map<String, String> body) {
        adminGuard.ensureAdmin(user.uid());
        moderation.warn(uid, body != null ? body.get("reason") : null);
        return moderation.summary(uid);
    }

    @PostMapping("/{uid}/suspend")
    public UserModerationService.ModerationView suspend(
            @AuthenticationPrincipal FirebaseUser user, @PathVariable String uid,
            @RequestBody(required = false) Map<String, Integer> body) {
        adminGuard.ensureAdmin(user.uid());
        int days = body != null && body.get("days") != null ? body.get("days") : 7;
        moderation.suspend(uid, days);
        return moderation.summary(uid);
    }

    @PostMapping("/{uid}/ban")
    public UserModerationService.ModerationView ban(
            @AuthenticationPrincipal FirebaseUser user, @PathVariable String uid) {
        adminGuard.ensureAdmin(user.uid());
        moderation.ban(uid);
        return moderation.summary(uid);
    }

    @PostMapping("/{uid}/unban")
    public UserModerationService.ModerationView unban(
            @AuthenticationPrincipal FirebaseUser user, @PathVariable String uid) {
        adminGuard.ensureAdmin(user.uid());
        moderation.unban(uid);
        return moderation.summary(uid);
    }
}
