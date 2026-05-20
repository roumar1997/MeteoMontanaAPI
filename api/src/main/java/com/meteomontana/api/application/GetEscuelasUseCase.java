package com.meteomontana.api.application;
import com.meteomontana.api.domain.model.Escuela;
import com.meteomontana.api.domain.port.EscuelaRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class GetEscuelasUseCase {
    private final EscuelaRepository repository;
    public GetEscuelasUseCase(EscuelaRepository repository){
        this.repository = repository;
    }

    public List<Escuela> execute(){
        return repository.findAll();
    }
}
