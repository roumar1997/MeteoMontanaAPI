package com.meteomontana.api.domain.exception;

/**
 * Petición inválida por reglas de negocio (→ 400): parámetros incoherentes,
 * estado no permitido, límites superados. Distinto de la validación
 * declarativa (@Valid), que la maneja MethodArgumentNotValidException.
 * El mapeo a HTTP vive en GlobalExceptionHandler.
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
