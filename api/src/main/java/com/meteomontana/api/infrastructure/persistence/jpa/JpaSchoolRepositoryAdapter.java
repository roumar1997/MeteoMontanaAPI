package com.meteomontana.api.infrastructure.persistence.jpa;
import com.meteomontana.api.domain.model.Escuela;
import com.meteomontana.api.domain.port.EscuelaRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class JpaSchoolRepositoryAdapter implements  EscuelaRepository {
    private final SpringDataSchoolRepository jpaRepo;
    public JpaSchoolRepositoryAdapter (SpringDataSchoolRepository jpaRepo){
        this.jpaRepo = jpaRepo;
    }

    @Override
    public List<Escuela> findAll() {
        return jpaRepo.findAll()
                .stream()
                .map(this::toEscuela)
                .toList();
    }
        @Override
        public Optional<Escuela> findById(String id) {
            return jpaRepo.findById(id)
                    .map(this::toEscuela);
        }

        private Escuela toEscuela(SchoolJpaEntity e) {
            return new Escuela(
                    e.getId(),
                    e.getName(),
                    e.getLocation(),
                    e.getRegion(),
                    e.getStyle(),
                    e.getRockType(),
                    e.getLat(),
                    e.getLon(),
                    e.getSource()
            );
        }


}
