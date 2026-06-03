package com.meteomontana.api.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PhotoNotFoundException extends RuntimeException {
    public PhotoNotFoundException(String id) {
        super("Photo not found: " + id);
    }
}
