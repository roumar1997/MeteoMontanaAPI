package com.meteomontana.api.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class SchoolNotFoundException extends RuntimeException {
    public SchoolNotFoundException(String id) {
        super("School not found: " + id);
    }
}
