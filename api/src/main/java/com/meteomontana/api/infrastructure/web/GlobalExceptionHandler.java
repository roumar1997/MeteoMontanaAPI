package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.domain.exception.ForbiddenException;
import com.meteomontana.api.domain.exception.PhotoNotFoundException;
import com.meteomontana.api.domain.exception.SchoolNotFoundException;
import com.meteomontana.api.domain.exception.UserNotFoundException;
import com.meteomontana.api.domain.exception.UsernameAlreadyTakenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Punto ÚNICO de traducción excepción → respuesta HTTP.
 *
 * Regla de arquitectura: el dominio lanza excepciones PURAS (sin saber de
 * HTTP) y este advice decide el status y el formato. Así los use cases se
 * pueden reutilizar desde schedulers/workers sin arrastrar Spring Web, y
 * TODOS los errores salen con la misma forma.
 *
 * Retrocompatibilidad con las apps: usan expectSuccess=true en Ktor (les
 * basta el status); el campo "message" se mantiene por si alguna pantalla
 * lo muestra. NUNCA filtrar mensajes internos en el caso genérico.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Forma única de TODOS los errores de la API. */
    public record ApiError(String code, String message, Instant timestamp) {
        static ApiError of(String code, String message) {
            return new ApiError(code, message, Instant.now());
        }
    }

    // ── Excepciones de dominio (el dominio no sabe de HTTP; el mapeo vive aquí)

    @ExceptionHandler({SchoolNotFoundException.class, PhotoNotFoundException.class,
            UserNotFoundException.class})
    public ResponseEntity<ApiError> notFound(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> forbidden(ForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of("FORBIDDEN", e.getMessage()));
    }

    @ExceptionHandler(UsernameAlreadyTakenException.class)
    public ResponseEntity<ApiError> conflict(UsernameAlreadyTakenException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of("CONFLICT", e.getMessage()));
    }

    // ── Validación declarativa (@Valid en los request DTOs) → 400 con detalle

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException e) {
        String fields = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of("VALIDATION", fields));
    }

    // ── ResponseStatusException (legado en controllers/use cases): respeta su
    //    status pero unifica el formato. Se irán migrando a excepciones de
    //    dominio; mientras tanto salen con la misma forma.

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> statusException(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(ApiError.of("ERROR", e.getReason() != null ? e.getReason() : "Error"));
    }

    // ── Red de seguridad: cualquier otra excepción → 500 con mensaje NEUTRO
    //    (el detalle SOLO al log del servidor, jamás al cliente).

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception e) {
        log.error("Error interno no controlado", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of("INTERNAL", "Error interno"));
    }
}
