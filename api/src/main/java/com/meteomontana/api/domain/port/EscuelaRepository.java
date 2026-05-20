package com.meteomontana.api.domain.port;
import com.meteomontana.api.domain.model.Escuela;
import java.util.List;
import java.util.Optional;

public interface EscuelaRepository {
    List<Escuela> findAll();
    Optional<Escuela> findById(String id);

}
