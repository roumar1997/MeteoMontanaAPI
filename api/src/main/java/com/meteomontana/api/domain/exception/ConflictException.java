package com.meteomontana.api.domain.exception;

/**
 * Conflicto con el estado actual (→ 409): p.ej. una contribución ya revisada,
 * un recurso duplicado. El mapeo a HTTP vive en GlobalExceptionHandler.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
