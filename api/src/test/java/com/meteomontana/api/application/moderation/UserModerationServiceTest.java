package com.meteomontana.api.application.moderation;

import com.meteomontana.api.infrastructure.persistence.jpa.UserJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataUserRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataContentReportRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.MeetupReportJpaRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataModerationActionRepository;
import com.meteomontana.api.domain.port.PushSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * UserModerationService.ensureCanPost — el cortafuegos que impide publicar a
 * usuarios baneados o suspendidos (lo llaman crear quedada/nota/comentario/foto).
 * Es seguridad: si se relaja, un suspendido volvería a publicar. También los
 * guardas anti auto-baneo/suspensión.
 */
class UserModerationServiceTest {

    private SpringDataUserRepository users;
    private UserModerationService svc;

    @BeforeEach void setUp() {
        users = mock(SpringDataUserRepository.class);
        svc = new UserModerationService(
                users,
                mock(SpringDataContentReportRepository.class),
                mock(MeetupReportJpaRepository.class),
                mock(SpringDataModerationActionRepository.class),
                mock(PushSender.class));
    }

    private UserJpaEntity user(boolean banned, LocalDateTime suspendedUntil) {
        var u = new UserJpaEntity("u", "e@x.com", "yo", "Yo", null, null, true, null,
                false, false, null, null, LocalDateTime.now(), LocalDateTime.now());
        u.setBanned(banned);
        u.setSuspendedUntil(suspendedUntil);
        return u;
    }

    @Test void baneado_noPuedePublicar() {
        when(users.findById("u")).thenReturn(Optional.of(user(true, null)));
        assertThatThrownBy(() -> svc.ensureCanPost("u"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test void suspendidoConFechaFutura_noPuedePublicar() {
        when(users.findById("u")).thenReturn(Optional.of(user(false, LocalDateTime.now().plusDays(3))));
        assertThatThrownBy(() -> svc.ensureCanPost("u"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test void suspensionYaVencida_puedePublicar() {
        when(users.findById("u")).thenReturn(Optional.of(user(false, LocalDateTime.now().minusDays(1))));
        svc.ensureCanPost("u");   // no lanza
    }

    @Test void usuarioLimpio_puedePublicar() {
        when(users.findById("u")).thenReturn(Optional.of(user(false, null)));
        svc.ensureCanPost("u");
    }

    @Test void uidNuloODesconocido_noBloquea() {
        svc.ensureCanPost(null);                       // sale sin tocar el repo
        when(users.findById("x")).thenReturn(Optional.empty());
        svc.ensureCanPost("x");                        // usuario inexistente → no bloquea
    }

    @Test void noPuedesSuspenderteATiMismo() {
        assertThatThrownBy(() -> svc.suspend("admin", "admin", 7, "x"))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(users);   // ni siquiera busca al usuario
    }

    @Test void noPuedesBanearteATiMismo() {
        assertThatThrownBy(() -> svc.ban("admin", "admin", "x"))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(users);
    }
}
