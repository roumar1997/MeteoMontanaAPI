package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.admin.AdminGuard;
import com.meteomontana.api.domain.port.NoteRepository;
import com.meteomontana.api.domain.port.UserRepository;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * Listas para el panel de admin (STATS pulsables): usuarios y notas más
 * recientes. Solo lectura sobre los PUERTOS de dominio; las acciones (borrar
 * nota, etc.) van por las rutas ya existentes (denuncias, notas...).
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminBrowseController {

    private static final int PAGE = 200;

    public record AdminUserRow(String uid, String username, String displayName,
                               boolean isAdmin, LocalDateTime createdAt) {}
    public record AdminNoteRow(String id, String schoolId, String author,
                               String uid, String text, LocalDateTime createdAt) {}

    private final AdminGuard adminGuard;
    private final UserRepository users;
    private final NoteRepository notes;

    /** Últimos 200 usuarios registrados (más recientes primero). */
    @GetMapping("/users")
    public List<AdminUserRow> users(@AuthenticationPrincipal FirebaseUser user) {
        adminGuard.ensureAdmin(user.uid());
        return users.findRecent(PAGE).stream()
                .map(u -> new AdminUserRow(u.getUid(), u.getUsername(), u.getDisplayName(),
                        u.isAdmin(), u.getCreatedAt()))
                .toList();
    }

    /** Últimas 200 notas comunitarias (más recientes primero). */
    @GetMapping("/notes")
    public List<AdminNoteRow> notes(@AuthenticationPrincipal FirebaseUser user) {
        adminGuard.ensureAdmin(user.uid());
        return notes.findRecent(PAGE).stream()
                .map(n -> new AdminNoteRow(n.getId(), n.getSchoolId(), n.getAuthor(),
                        n.getUid(), n.getText(), n.getCreatedAt()))
                .toList();
    }
}
