package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.admin.AdminGuard;
import com.meteomontana.api.application.contribution.ContributionRequest;
import com.meteomontana.api.application.contribution.ContributionResponse;
import com.meteomontana.api.application.contribution.ReviewContributionUseCase;
import com.meteomontana.api.application.contribution.SubmitContributionUseCase;
import com.meteomontana.api.domain.port.PendingContributionRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ContributionController.submit vía MockMvc standalone. Verifica que el @Valid
 * está REALMENTE cableado en el endpoint (no solo en el DTO): un bloquesJson
 * gigante o unas coords imposibles rebotan con 400 ANTES de tocar el use case
 * (que persiste en prod). Es la defensa M1 vista de punta a punta.
 */
class ContributionControllerTest {

    private SubmitContributionUseCase submit;
    private MockMvc mvc;

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
        submit = mock(SubmitContributionUseCase.class);
        var review = mock(ReviewContributionUseCase.class);
        var repo = mock(PendingContributionRepository.class);
        var guard = mock(AdminGuard.class);
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new ContributionController(submit, review, repo, guard))
                .setCustomArgumentResolvers(new FakePrincipal())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                // Jackson explícito (Spring Boot 3.5.16 dejó de registrarlo por
                // defecto en standaloneSetup → el body salía como String).
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter())
                .build();
    }

    private String body(String bloquesJson, double lat) {
        return "{\"type\":\"BOULDER\",\"lat\":" + lat + ",\"lon\":-3.0,\"bloquesJson\":\""
                + bloquesJson + "\"}";
    }

    @Test void contribucionValida_delegaEnElUseCase() throws Exception {
        when(submit.execute(eq("esc-1"), any(ContributionRequest.class), any()))
                .thenReturn(mock(ContributionResponse.class));
        mvc.perform(post("/api/schools/esc-1/contributions").contentType("application/json")
                        .content(body("[]", 40.5)))
                .andExpect(status().isCreated());
        verify(submit).execute(eq("esc-1"), any(), any());
    }

    @Test void bloquesJsonGigante_da400YNoTocaElUseCase() throws Exception {
        mvc.perform(post("/api/schools/esc-1/contributions").contentType("application/json")
                        .content(body("x".repeat(300_000), 40.5)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
        verifyNoInteractions(submit);
    }

    @Test void latitudImposible_da400() throws Exception {
        mvc.perform(post("/api/schools/esc-1/contributions").contentType("application/json")
                        .content(body("[]", 999.0)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(submit);
    }
}
