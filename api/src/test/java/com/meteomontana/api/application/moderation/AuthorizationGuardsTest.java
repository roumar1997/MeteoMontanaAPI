package com.meteomontana.api.application.moderation;

import com.meteomontana.api.application.admin.AdminGuard;
import com.meteomontana.api.application.photos.DeleteSchoolPhotoUseCase;
import com.meteomontana.api.domain.exception.ForbiddenException;
import com.meteomontana.api.domain.model.SchoolPhoto;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.SchoolPhotoRepository;
import com.meteomontana.api.domain.port.UserRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataUserRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.UserJpaEntity;
import com.meteomontana.api.infrastructure.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La lógica de AUTORIZACIÓN más sensible del sistema, hasta ahora sin tests
 * dedicados (hallazgo de la re-auditoría 2026-07-19): rol admin, cortafuegos
 * de baneo/suspensión y propiedad de las fotos. Una regresión aquí se
 * despliega a prod con usuarios reales — por eso cada regla tiene su test.
 */
class AuthorizationGuardsTest {

    // ── AdminGuard ─────────────────────────────────────────────────────────

    UserRepository userRepo = mock(UserRepository.class);
    AdminGuard guard = new AdminGuard(userRepo);

    private User userWithAdmin(boolean admin) {
        User u = mock(User.class);
        when(u.isAdmin()).thenReturn(admin);
        return u;
    }

    @Test
    void nonAdminIsRejected() {
        var u = userWithAdmin(false);
        when(userRepo.findByUid("u1")).thenReturn(Optional.of(u));
        assertThrows(ForbiddenException.class, () -> guard.ensureAdmin("u1"));
    }

    @Test
    void unknownUidIsRejected() {
        when(userRepo.findByUid("ghost")).thenReturn(Optional.empty());
        assertThrows(ForbiddenException.class, () -> guard.ensureAdmin("ghost"));
    }

    @Test
    void adminPasses() {
        var u = userWithAdmin(true);
        when(userRepo.findByUid("boss")).thenReturn(Optional.of(u));
        assertDoesNotThrow(() -> guard.ensureAdmin("boss"));
    }

    // ── UserModerationService.ensureCanPost ────────────────────────────────

    SpringDataUserRepository users = mock(SpringDataUserRepository.class);
    UserModerationService moderation;

    @BeforeEach
    void setUpModeration() {
        moderation = new UserModerationService(users,
                mock(com.meteomontana.api.infrastructure.persistence.jpa.SpringDataContentReportRepository.class),
                mock(com.meteomontana.api.infrastructure.persistence.jpa.MeetupReportJpaRepository.class),
                mock(com.meteomontana.api.infrastructure.persistence.jpa.SpringDataModerationActionRepository.class),
                mock(com.meteomontana.api.domain.port.PushSender.class));
    }

    private UserJpaEntity mockUser(boolean banned, LocalDateTime suspendedUntil) {
        UserJpaEntity u = mock(UserJpaEntity.class);
        when(u.isBanned()).thenReturn(banned);
        when(u.getSuspendedUntil()).thenReturn(suspendedUntil);
        return u;
    }

    @Test
    void bannedUserCannotPost() {
        var u = mockUser(true, null);
        when(users.findById("bad")).thenReturn(Optional.of(u));
        assertThrows(ForbiddenException.class, () -> moderation.ensureCanPost("bad"));
    }

    @Test
    void suspendedUserCannotPostUntilDatePasses() {
        var u = mockUser(false, LocalDateTime.now().plusDays(3));
        when(users.findById("sus")).thenReturn(Optional.of(u));
        assertThrows(ForbiddenException.class, () -> moderation.ensureCanPost("sus"));
    }

    @Test
    void expiredSuspensionPostsAgain() {
        var u = mockUser(false, LocalDateTime.now().minusDays(1));
        when(users.findById("ok")).thenReturn(Optional.of(u));
        assertDoesNotThrow(() -> moderation.ensureCanPost("ok"));
    }

    @Test
    void normalUserPosts() {
        var u = mockUser(false, null);
        when(users.findById("u")).thenReturn(Optional.of(u));
        assertDoesNotThrow(() -> moderation.ensureCanPost("u"));
    }

    // ── DeleteSchoolPhotoUseCase: solo el que subió la foto borra ──────────

    SchoolPhotoRepository photos = mock(SchoolPhotoRepository.class);
    StorageService storage = mock(StorageService.class);
    DeleteSchoolPhotoUseCase deletePhoto = new DeleteSchoolPhotoUseCase(photos, storage);

    private SchoolPhoto photoOf(String uploaderUid) {
        SchoolPhoto p = mock(SchoolPhoto.class);
        when(p.getUploadedByUid()).thenReturn(uploaderUid);
        when(p.getStoragePath()).thenReturn("schools/x/1.jpg");
        return p;
    }

    @Test
    void deletingSomeoneElsesPhotoIsForbidden() {
        var p = photoOf("owner");
        when(photos.findById("p1")).thenReturn(Optional.of(p));
        assertThrows(ForbiddenException.class, () -> deletePhoto.execute("p1", "intruso"));
        verify(photos, never()).deleteById(any());
    }

    @Test
    void ownerDeletesOwnPhoto() {
        var p = photoOf("owner");
        when(photos.findById("p1")).thenReturn(Optional.of(p));
        assertDoesNotThrow(() -> deletePhoto.execute("p1", "owner"));
        verify(photos).deleteById("p1");
    }
}
