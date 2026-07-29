package com.meteomontana.api.domain.exception;

/**
 * Recurso no encontrado (→ 404). Excepción de dominio genérica para los casos
 * que no tienen una excepción específica (SchoolNotFoundException, etc.).
 * El mapeo a HTTP vive en GlobalExceptionHandler; el dominio no sabe de HTTP.
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
