package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.domain.exception.ForbiddenException;
import com.meteomontana.api.domain.exception.SchoolNotFoundException;
import com.meteomontana.api.domain.exception.UsernameAlreadyTakenException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * El GlobalExceptionHandler (Bloque 1): TODO error de la API sale con la misma
 * forma {code, message, timestamp}. Verifica el mapeo por familia y —crítico
 * para seguridad— que el 500 genérico NO filtra el mensaje interno al cliente.
 *
 * Montaje standalone (sin contexto Spring ni seguridad): controlador de prueba
 * + el advice real. Rápido y aislado.
 */
class GlobalExceptionHandlerTest {

    /** Controlador de prueba que dispara cada familia de excepción. */
    @RestController
    static class BoomController {
        @GetMapping("/boom/notfound") String nf() { throw new SchoolNotFoundException("x"); }
        @GetMapping("/boom/forbidden") String fb() { throw new ForbiddenException("no puedes"); }
        @GetMapping("/boom/conflict") String cf() { throw new UsernameAlreadyTakenException("pillado"); }
        @GetMapping("/boom/status") String st() {
            throw new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT, "soy tetera");
        }
        @GetMapping("/boom/unexpected") String ue() {
            throw new IllegalStateException("SECRETO INTERNO: password=1234");
        }
        record Body(@NotBlank String name) {}
        @PostMapping("/boom/valid") String vl(@Valid @RequestBody Body b) { return b.name(); }
    }

    private MockMvc mvc;

    @BeforeEach void setUp() {
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new BoomController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test void notFound_da404ConCodeNOT_FOUND() throws Exception {
        mvc.perform(get("/boom/notfound"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test void forbidden_da403() throws Exception {
        mvc.perform(get("/boom/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("no puedes"));
    }

    @Test void usernameTaken_da409() throws Exception {
        mvc.perform(get("/boom/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test void responseStatusException_respetaElStatus() throws Exception {
        mvc.perform(get("/boom/status"))
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.code").value("ERROR"))
                .andExpect(jsonPath("$.message").value("soy tetera"));
    }

    @Test void bodyInvalido_da400ConCodeVALIDATION() throws Exception {
        mvc.perform(post("/boom/valid").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("name")));
    }

    @Test void errorInesperado_da500YNoFiltraElMensajeInterno() throws Exception {
        mvc.perform(get("/boom/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL"))
                .andExpect(jsonPath("$.message").value("Error interno"))
                // El secreto interno JAMÁS debe llegar al cliente.
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("SECRETO"))));
    }
}
