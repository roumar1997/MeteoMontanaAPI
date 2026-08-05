package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.moderation.ContentModerationService;
import com.meteomontana.api.application.admin.AdminGuard;
import com.meteomontana.api.application.social.FollowUseCase;
import com.meteomontana.api.application.users.UserIdentifierResolver;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.UserRepository;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Al tocar una mención {@code @usuario} en el feed la app solo conoce el
 * username, y con él abre el perfil entero. Estas pruebas fijan que todo lo
 * que cuelga de ese perfil acepta las DOS formas — uid y username — y siempre
 * llega al caso de uso con el uid real. Un endpoint nuevo que se olvide del
 * resolver rompe aquí.
 */
class FollowByUsernameTest {

    private static final String UID = "uid-karly";
    private static final String USERNAME = "karlyrubio";

    private FollowUseCase follow;
    private ContentModerationService moderation;
    private MockMvc follows;
    private MockMvc blocks;

    static class FakePrincipal implements HandlerMethodArgumentResolver {
        public boolean supportsParameter(MethodParameter p) {
            return p.getParameterType().equals(FirebaseUser.class);
        }
        public Object resolveArgument(MethodParameter p, ModelAndViewContainer m,
                                      NativeWebRequest w, WebDataBinderFactory b) {
            return new FirebaseUser("uid-yo", "yo@x.com", "Yo");
        }
    }

    @BeforeEach void setUp() {
        follow = mock(FollowUseCase.class);
        moderation = mock(ContentModerationService.class);
        UserRepository users = mock(UserRepository.class);
        User karly = new User(UID, "k@x.com", USERNAME, "Karly", null, null, true, null,
                false, false, null, null, null, null);
        when(users.findByUid(anyString())).thenReturn(Optional.empty());
        when(users.findByUsername(anyString())).thenReturn(Optional.empty());
        when(users.findByUid(UID)).thenReturn(Optional.of(karly));
        when(users.findByUsername(USERNAME)).thenReturn(Optional.of(karly));
        var resolver = new UserIdentifierResolver(users);

        when(follow.statusFor(anyString(), anyString()))
                .thenReturn(new FollowUseCase.FollowStatusDto(7, 3, true, false, false));
        when(follow.listFollowers(anyString())).thenReturn(List.of());
        when(follow.listFollowing(anyString())).thenReturn(List.of());

        follows = MockMvcBuilders.standaloneSetup(new FollowController(follow, resolver))
                .setCustomArgumentResolvers(new FakePrincipal())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter())
                .build();
        blocks = MockMvcBuilders.standaloneSetup(
                        new ModerationController(moderation, mock(AdminGuard.class), resolver))
                .setCustomArgumentResolvers(new FakePrincipal())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test void followStatusPorUsernameDevuelveLosContadoresReales() throws Exception {
        follows.perform(get("/api/users/" + USERNAME + "/follow-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followers").value(7))
                .andExpect(jsonPath("$.iFollowThem").value(true));
        verify(follow).statusFor("uid-yo", UID);
    }

    @Test void followStatusPorUidSigueIgual() throws Exception {
        follows.perform(get("/api/users/" + UID + "/follow-status")).andExpect(status().isOk());
        verify(follow).statusFor("uid-yo", UID);
    }

    @Test void seguirPorUsernameLlegaConElUid() throws Exception {
        follows.perform(post("/api/users/" + USERNAME + "/follow")).andExpect(status().isCreated());
        verify(follow).follow("uid-yo", UID);
    }

    @Test void dejarDeSeguirPorUsernameLlegaConElUid() throws Exception {
        follows.perform(delete("/api/users/" + USERNAME + "/follow")).andExpect(status().isNoContent());
        verify(follow).unfollow("uid-yo", UID);
    }

    @Test void listasDeSeguidoresYSeguidosPorUsername() throws Exception {
        follows.perform(get("/api/users/" + USERNAME + "/followers")).andExpect(status().isOk());
        follows.perform(get("/api/users/" + USERNAME + "/following")).andExpect(status().isOk());
        verify(follow).listFollowers(UID);
        verify(follow).listFollowing(UID);
    }

    @Test void bloquearYDesbloquearPorUsername() throws Exception {
        blocks.perform(post("/api/users/" + USERNAME + "/block")).andExpect(status().isNoContent());
        blocks.perform(delete("/api/users/" + USERNAME + "/block")).andExpect(status().isNoContent());
        verify(moderation).block("uid-yo", UID);
        verify(moderation).unblock("uid-yo", UID);
    }

    @Test void usuarioDesconocidoEs404YNoTocaElCasoDeUso() throws Exception {
        follows.perform(post("/api/users/nadie/follow")).andExpect(status().isNotFound());
        verify(follow, never()).follow(anyString(), anyString());
    }
}
