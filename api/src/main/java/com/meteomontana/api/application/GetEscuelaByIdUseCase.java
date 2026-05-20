package com.meteomontana.api.application;


import com.meteomontana.api.domain.model.Escuela;
import com.meteomontana.api.domain.port.EscuelaRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class GetEscuelaByIdUseCase {

    private final EscuelaRepository repository;
    public GetEscuelaByIdUseCase(EscuelaRepository repository) {
        this.repository = repository;
    }

    public Optional<Escuela> execute(String id) {
        return repository.findById(id);
    }
}
