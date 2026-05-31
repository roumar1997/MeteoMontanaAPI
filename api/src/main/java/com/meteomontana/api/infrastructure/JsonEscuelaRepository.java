package com.meteomontana.api.infrastructure;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meteomontana.api.domain.model.Escuela;
import com.meteomontana.api.domain.port.EscuelaRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;


@Repository
public class JsonEscuelaRepository implements EscuelaRepository {

    private final List<Escuela> escuelas;

    public JsonEscuelaRepository() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("escuelas.json")){
            if (in == null) {
                throw new IOException("no se encontró .json en resources");
            }
            this.escuelas = mapper.readValue(in, new TypeReference<List<Escuela>>(){});
        }
    }
    @Override
    public List<Escuela> findAll(){
        return escuelas;


    }
    @Override
    public Optional<Escuela> findById(String id) {
        return escuelas.stream()
                .filter(e -> e.getId().equals(id))
                .findFirst();
    }
}

