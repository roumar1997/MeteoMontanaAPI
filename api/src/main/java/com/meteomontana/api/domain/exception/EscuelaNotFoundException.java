package com.meteomontana.api.domain.exception;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;



@ResponseStatus(HttpStatus.NOT_FOUND)
public class EscuelaNotFoundException extends  RuntimeException{
    public EscuelaNotFoundException(String id) {
        super ( "Escuela no encontrada: "+ id);
    }
}
