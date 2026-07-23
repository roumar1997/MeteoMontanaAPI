package com.meteomontana.api.domain.exception;


public class SchoolNotFoundException extends RuntimeException {
    public SchoolNotFoundException(String id) {
        super("School not found: " + id);
    }
}
