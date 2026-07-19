package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.feed.FeedService;
import com.meteomontana.api.domain.port.UserRepository;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * FeedController vía MockMvc standalone (sin contexto Spring ni Firebase): un
 * resolver falso inyecta el FirebaseUser autenticado, el servicio va mockeado.
 * Verifica ROUTING + wiring de @Valid + delegación al servicio — la capa que el
 * estudio de arquitectura señaló como "0 controllers testeados".
 */
class FeedControllerTest {

    private FeedService service;
    private MockMvc mvc;

    /** Simula @AuthenticationPrincipal FirebaseUser sin cargar Spring Security. */
    static class FakePrincipal implements HandlerMethodArgumentResolver {
        public boolean supportsParameter(MethodParameter p) {
            return p.getParameterType().equals(FirebaseUser.class);
        }
        public Object resolveArgument(MethodParameter p, ModelAndViewContainer m,
                                      NativeWebRequest w, WebDataBinderFactory b) {
            return new FirebaseUser("uid-test", "e@x.com", "Test");
        }
    }

    @BeforeEach void setUp() {
        service = mock(FeedService.class);
        UserRepository users = mock(UserRepository.class);
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new FeedController(service, users))
                .setCustomArgumentResolvers(new FakePrincipal())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test void publish_valido_delegaEnElServicioYDevuelveId() throws Exception {
        when(service.publish(eq("uid-test"), eq("block-1"), isNull(), eq("TICK"), any(), any()))
                .thenReturn(99L);
        mvc.perform(post("/api/feed").contentType("application/json")
                        .content("{\"blockId\":\"block-1\",\"kind\":\"TICK\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(99));
        verify(service).publish("uid-test", "block-1", null, "TICK", null, null);
    }

    @Test void publish_sinBlockId_da400YNoTocaElServicio() throws Exception {
        mvc.perform(post("/api/feed").contentType("application/json")
                        .content("{\"kind\":\"TICK\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
        verifyNoInteractions(service);
    }

    @Test void publish_conCaptionGigante_da400() throws Exception {
        String caption = "x".repeat(600);   // > 500
        mvc.perform(post("/api/feed").contentType("application/json")
                        .content("{\"blockId\":\"b1\",\"kind\":\"TICK\",\"caption\":\"" + caption + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void comentario_vacio_da400() throws Exception {
        mvc.perform(post("/api/feed/5/comments").contentType("application/json")
                        .content("{\"text\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }

    @Test void like_delegaYDevuelveContador() throws Exception {
        when(service.like("uid-test", 7L)).thenReturn(3L);
        mvc.perform(post("/api/feed/7/like"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(3));
    }

    @Test void page_usaElUidAutenticadoYElScope() throws Exception {
        when(service.page(eq("uid-test"), eq("all"), isNull(), eq(20)))
                .thenReturn(java.util.List.of());
        mvc.perform(get("/api/feed"))
                .andExpect(status().isOk());
        verify(service).page("uid-test", "all", null, 20);
    }
}
