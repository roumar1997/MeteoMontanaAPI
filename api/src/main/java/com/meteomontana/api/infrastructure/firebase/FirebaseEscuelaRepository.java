package com.meteomontana.api.infrastructure.firebase;

import com.meteomontana.api.domain.model.Escuela;
import com.meteomontana.api.domain.port.EscuelaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public class FirebaseEscuelaRepository implements EscuelaRepository {

    @Override
    public List<Escuela> findAll() {
        // TODO: reimplementar cuando se decida usar Firebase para escuelas
        return List.of();
    }

    @Override
    public Optional<Escuela> findById(String id) {
        return Optional.empty();
    }
}
