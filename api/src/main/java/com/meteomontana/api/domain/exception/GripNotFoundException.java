package com.meteomontana.api.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class GripNotFoundException extends RuntimeException {
    public GripNotFoundException(String message) {
        super(message);
    }
}
