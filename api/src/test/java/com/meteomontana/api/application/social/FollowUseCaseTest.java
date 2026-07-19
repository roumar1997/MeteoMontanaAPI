package com.meteomontana.api.application.social;

import com.meteomontana.api.application.users.PublicProfileDto;
import com.meteomontana.api.application.users.UserDtoMapper;
import com.meteomontana.api.domain.exception.ForbiddenException;
import com.meteomontana.api.domain.exception.UserNotFoundException;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.FollowRepository;
import com.meteomontana.api.domain.port.PushSender;
import com.meteomontana.api.domain.port.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FollowUseCase.follow — el modelo de privacidad social: seguir a una cuenta
 * PÚBLICA es directo (ACCEPTED + "nuevo seguidor"); a una PRIVADA crea una
 * SOLICITUD (PENDING + "quiere seguirte"). Además: no seguirse a sí mismo,
 * usuario inexistente 404, e idempotencia (ya siguiendo/pendiente = no-op).
 */
class FollowUseCaseTest {

    private FollowRepository follows;
    private UserRepository users;
    private NotificationService notifs;
    private FollowUseCase useCase;

    private User user(String uid, boolean isPublic) {
        return new User(uid, "e@x.com", "u_" + uid, "Nombre", null, null, isPublic, null,
                false, false, null, null, LocalDateTime.now(), LocalDateTime.now());
    }

    @BeforeEach void setUp() {
        follows = mock(FollowRepository.class);
        users = mock(UserRepository.class);
        notifs = mock(NotificationService.class);
        UserDtoMapper mapper = mock(UserDtoMapper.class);
        when(mapper.toPublic(any())).thenReturn(
                new PublicProfileDto("x", "u", "N", null, null, null, false, false));
        useCase = new FollowUseCase(follows, users, notifs, mock(PushSender.class), mapper);
        when(users.findByUid("me")).thenReturn(Optional.of(user("me", true)));
    }

    @Test void noPuedesSeguirteATiMismo() {
        assertThatThrownBy(() -> useCase.follow("me", "me"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test void usuarioInexistente_da404() {
        when(users.findByUid("nadie")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.follow("me", "nadie"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test void seguirCuentaPublica_esAceptadoDirecto() {
        when(users.findByUid("pub")).thenReturn(Optional.of(user("pub", true)));
        when(follows.isFollowing("me", "pub")).thenReturn(false);
        when(follows.hasPendingRequest("me", "pub")).thenReturn(false);

        useCase.follow("me", "pub");

        verify(follows).add("me", "pub", "ACCEPTED");
        verify(notifs).create(eq("pub"), eq("NEW_FOLLOWER"), any(), any(), eq("user"), eq("me"));
    }

    @Test void seguirCuentaPrivada_creaSolicitudPendiente() {
        when(users.findByUid("priv")).thenReturn(Optional.of(user("priv", false)));
        when(follows.isFollowing("me", "priv")).thenReturn(false);
        when(follows.hasPendingRequest("me", "priv")).thenReturn(false);

        useCase.follow("me", "priv");

        verify(follows).add("me", "priv", "PENDING");
        verify(notifs).create(eq("priv"), eq("FOLLOW_REQUEST"), any(), any(), eq("follow_request"), eq("me"));
    }

    @Test void yaSiguiendo_noHaceNada() {
        when(users.findByUid("pub")).thenReturn(Optional.of(user("pub", true)));
        when(follows.isFollowing("me", "pub")).thenReturn(true);

        useCase.follow("me", "pub");

        verify(follows, never()).add(any(), any(), any());
        verifyNoInteractions(notifs);
    }

    @Test void solicitudYaPendiente_noHaceNada() {
        when(users.findByUid("priv")).thenReturn(Optional.of(user("priv", false)));
        when(follows.isFollowing("me", "priv")).thenReturn(false);
        when(follows.hasPendingRequest("me", "priv")).thenReturn(true);

        useCase.follow("me", "priv");

        verify(follows, never()).add(any(), any(), any());
    }
}
