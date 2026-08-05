package com.meteomontana.api.application.users;

import com.meteomontana.api.domain.exception.UserNotFoundException;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * El identificador que llega por la API puede ser uid o username: las apps
 * abren perfiles desde menciones @usuario y ahí solo tienen el username.
 */
class UserIdentifierResolverTest {

    private UserRepository users;
    private UserIdentifierResolver resolver;

    private static User user(String uid, String username) {
        return new User(uid, "a@b.com", username, "Karly", null, null, true, null,
                false, false, null, null, null, null);
    }

    @BeforeEach void setUp() {
        users = mock(UserRepository.class);
        when(users.findByUid(anyString())).thenReturn(Optional.empty());
        when(users.findByUsername(anyString())).thenReturn(Optional.empty());
        resolver = new UserIdentifierResolver(users);
    }

    @Test void resuelvePorUid() {
        when(users.findByUid("uid-1")).thenReturn(Optional.of(user("uid-1", "karlyrubio")));
        assertThat(resolver.requireUid("uid-1")).isEqualTo("uid-1");
        verify(users, never()).findByUsername(anyString());
    }

    @Test void resuelvePorUsernameCuandoNoEsUnUid() {
        when(users.findByUsername("karlyrubio")).thenReturn(Optional.of(user("uid-1", "karlyrubio")));
        assertThat(resolver.requireUid("karlyrubio")).isEqualTo("uid-1");
    }

    @Test void desconocidoEs404() {
        assertThatThrownBy(() -> resolver.requireUid("nadie"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test void nuloOVacioNoLlegaAlRepositorio() {
        assertThat(resolver.find(null)).isEmpty();
        assertThat(resolver.find("  ")).isEmpty();
        verifyNoInteractions(users);
    }

    @Test void uidOrSameDevuelveElIdentificadorSiNoExiste() {
        assertThat(resolver.uidOrSame("nadie")).isEqualTo("nadie");
    }

    @Test void uidOrSameTraduceElUsername() {
        when(users.findByUsername("karlyrubio")).thenReturn(Optional.of(user("uid-1", "karlyrubio")));
        assertThat(resolver.uidOrSame("karlyrubio")).isEqualTo("uid-1");
    }
}
