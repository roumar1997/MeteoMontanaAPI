package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.contribution.ContributionRequest;
import com.meteomontana.api.application.meetups.CreateMeetupRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bean Validation de los request DTOs (los @Size/@DecimalMin/@NotBlank que
 * añadimos en el Bloque 1). Estos límites son la defensa M1: sin ellos un
 * usuario autenticado podía escribir megabytes en columnas TEXT o mandar
 * coordenadas imposibles. Este test comprueba que las anotaciones EXISTEN y
 * DISPARAN — si alguien las quita, se pone rojo.
 */
class RequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll static void init() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    @AfterAll static void close() { factory.close(); }

    private ContributionRequest contribution(String bloquesJson, double lat, String type) {
        return new ContributionRequest(
                type, "nombre", lat, -3.0, "notas", "desc", null, null, null,
                null, null, null, null, bloquesJson, null, "BOULDER", "POINT", null, "LTR", null);
    }

    @Test
    void contribucionValida_noTieneViolaciones() {
        var v = validator.validate(contribution("[]", 40.5, "BOULDER"));
        assertTrue(v.isEmpty(), "una contribución normal no debe tener violaciones: " + v);
    }

    @Test
    void bloquesJsonGigante_esRechazado() {
        String enorme = "x".repeat(300_000);   // > 256 KB
        var v = validator.validate(contribution(enorme, 40.5, "BOULDER"));
        assertFalse(v.isEmpty(), "un bloquesJson de 300 KB debe violar @Size");
        assertTrue(v.stream().anyMatch(c -> c.getPropertyPath().toString().equals("bloquesJson")));
    }

    @Test
    void latitudImposible_esRechazada() {
        var v = validator.validate(contribution("[]", 999.0, "BOULDER"));
        assertTrue(v.stream().anyMatch(c -> c.getPropertyPath().toString().equals("lat")),
                "lat=999 debe violar @DecimalMax(90)");
    }

    @Test
    void typeEnBlanco_esRechazado() {
        var v = validator.validate(contribution("[]", 40.0, ""));
        assertTrue(v.stream().anyMatch(c -> c.getPropertyPath().toString().equals("type")),
                "type vacío debe violar @NotBlank");
    }

    @Test
    void quedadaValida_noTieneViolaciones() {
        var req = new CreateMeetupRequest("esc-1", "Quedada", "desc", "BOULDER", "OPEN",
                4, null, List.of(java.time.LocalDate.of(2026, 7, 25)));
        assertTrue(validator.validate(req).isEmpty());
    }

    @Test
    void quedadaSinEscuela_esRechazada() {
        var req = new CreateMeetupRequest("", "Quedada", null, "BOULDER", "OPEN",
                4, null, List.of(java.time.LocalDate.of(2026, 7, 25)));
        Set<ConstraintViolation<CreateMeetupRequest>> v = validator.validate(req);
        assertTrue(v.stream().anyMatch(c -> c.getPropertyPath().toString().equals("schoolId")));
    }

    @Test
    void quedadaConAforoAbsurdo_esRechazada() {
        var req = new CreateMeetupRequest("esc-1", "Quedada", null, "BOULDER", "OPEN",
                99999, null, List.of(java.time.LocalDate.of(2026, 7, 25)));
        var v = validator.validate(req);
        assertTrue(v.stream().anyMatch(c -> c.getPropertyPath().toString().equals("memberLimit")),
                "aforo 99999 debe violar @Max(500)");
    }
}
