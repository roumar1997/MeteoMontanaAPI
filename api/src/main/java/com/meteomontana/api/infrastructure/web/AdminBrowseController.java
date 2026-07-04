package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.admin.AdminGuard;
import com.meteomontana.api.infrastructure.persistence.jpa.NoteJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataNoteRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataUserRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.UserJpaEntity;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Listas para el panel de admin (STATS pulsables): usuarios y notas más
 * recientes. Solo lectura; las acciones (borrar nota, etc.) van por las rutas
 * ya existentes (denuncias, notas...).
 */
@RestController
@RequestMapping("/api/admin")
public class AdminBrowseController {

    public record AdminUserRow(String uid, String username, String displayName,
                               boolean isAdmin, LocalDateTime createdAt) {}
    public record AdminNoteRow(String id, String schoolId, String author,
                               String uid, String text, LocalDateTime createdAt) {}

    private final AdminGuard adminGuard;
    private final SpringDataUserRepository users;
    private final SpringDataNoteRepository notes;

    public AdminBrowseController(AdminGuard adminGuard,
                                 SpringDataUserRepository users,
                                 SpringDataNoteRepository notes) {
        this.adminGuard = adminGuard;
        this.users = users;
        this.notes = notes;
    }

    /** Últimos 200 usuarios registrados (más recientes primero). */
    @GetMapping("/users")
    public List<AdminUserRow> users(@AuthenticationPrincipal FirebaseUser user) {
        adminGuard.ensureAdmin(user.uid());
        return users.findAll(PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .map((UserJpaEntity u) -> new AdminUserRow(
                        u.getUid(), u.getUsername(), u.getDisplayName(),
                        u.isAdmin(), u.getCreatedAt()))
                .toList();
    }

    /** Últimas 200 notas comunitarias (más recientes primero). */
    @GetMapping("/notes")
    public List<AdminNoteRow> notes(@AuthenticationPrincipal FirebaseUser user) {
        adminGuard.ensureAdmin(user.uid());
        return notes.findAll(PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .map((NoteJpaEntity n) -> new AdminNoteRow(
                        n.getId(), n.getSchool() != null ? n.getSchool().getId() : null, n.getAuthor(),
                        n.getUid(), n.getText(), n.getCreatedAt()))
                .toList();
    }
}
