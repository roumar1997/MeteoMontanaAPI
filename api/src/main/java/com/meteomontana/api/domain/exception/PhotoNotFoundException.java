package com.meteomontana.api.domain.exception;


public class PhotoNotFoundException extends RuntimeException {
    public PhotoNotFoundException(String id) {
        super("Photo not found: " + id);
    }
}
