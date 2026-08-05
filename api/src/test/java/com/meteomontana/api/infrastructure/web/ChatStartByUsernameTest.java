package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.moderation.ContentModerationService;
import com.meteomontana.api.application.users.UserIdentifierResolver;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.ChatRepository;
import com.meteomontana.api.domain.port.FollowRepository;
import com.meteomontana.api.domain.port.PushSender;
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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * El destinatario del chat puede llegar como uid o como username: el perfil
 * abierto desde una mención @usuario no conoce el uid. Sin resolver, /start
 * devolvía 404 y la conversación no se creaba nunca — chat vacío y mensaje
 * que no sale.
 */
class ChatStartByUsernameTest {

    private static final String UID = "uid-karly";
    private static final String USERNAME = "karlyrubio";

    private ChatRepository chats;
    private MockMvc mvc;

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
        UserRepository users = mock(UserRepository.class);
        User karly = new User(UID, "k@x.com", USERNAME, "Karly", null, null, true, null,
                false, false, null, null, null, null);
        when(users.findByUid(anyString())).thenReturn(Optional.empty());
        when(users.findByUsername(anyString())).thenReturn(Optional.empty());
        when(users.findByUid(UID)).thenReturn(Optional.of(karly));
        when(users.findByUsername(USERNAME)).thenReturn(Optional.of(karly));

        chats = mock(ChatRepository.class);
        var follows = mock(FollowRepository.class);
        var moderation = mock(ContentModerationService.class);
        when(moderation.eitherBlocked(anyString(), anyString())).thenReturn(false);

        mvc = MockMvcBuilders.standaloneSetup(new ChatPushController(
                        users, new UserIdentifierResolver(users), follows, chats,
                        mock(PushSender.class), moderation))
                .setCustomArgumentResolvers(new FakePrincipal())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test void abrirChatPorUsernameCreaLaConversacionConElUidReal() throws Exception {
        mvc.perform(post("/api/chat/start").contentType("application/json")
                        .content("{\"toUid\":\"" + USERNAME + "\"}"))
                .andExpect(status().isOk());
        verify(chats).ensureConversation("uid-yo", UID);
    }

    @Test void abrirChatPorUidSigueIgual() throws Exception {
        mvc.perform(post("/api/chat/start").contentType("application/json")
                        .content("{\"toUid\":\"" + UID + "\"}"))
                .andExpect(status().isOk());
        verify(chats).ensureConversation("uid-yo", UID);
    }

    @Test void destinatarioDesconocidoEs404YNoCreaNada() throws Exception {
        mvc.perform(post("/api/chat/start").contentType("application/json")
                        .content("{\"toUid\":\"nadie\"}"))
                .andExpect(status().isNotFound());
        verify(chats, never()).ensureConversation(anyString(), anyString());
    }
}
