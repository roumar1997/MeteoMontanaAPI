package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.School;
import com.meteomontana.api.domain.port.SchoolRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class JpaSchoolRepositoryAdapter implements SchoolRepository {

    private final SpringDataSchoolRepository jpaRepo;

    public JpaSchoolRepositoryAdapter(SpringDataSchoolRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    // El catálogo (191 escuelas) cambia muy poco; cachearlo evita leer la tabla
    // entera en cada petición de lista/búsqueda/prefetch. Se invalida al guardar
    // una escuela (alta/edición/aprobación de propuesta).
    @Override
    @Cacheable("schools-catalog")
    public List<School> findAll() {
        return jpaRepo.findAll()
                .stream()
                .map(this::toSchool)
                .toList();
    }

    @Override
    public Optional<School> findById(String id) {
        return jpaRepo.findById(id)
                .map(this::toSchool);
    }

    @Override
    @CacheEvict(value = "schools-catalog", allEntries = true)
    public School save(School s) {
        SchoolJpaEntity e = new SchoolJpaEntity(
                s.getId(), s.getName(), s.getLocation(), s.getRegion(),
                s.getStyle(), s.getRockType(), s.getLat(), s.getLon(), s.getSource()
        );
        return toSchool(jpaRepo.save(e));
    }

    private School toSchool(SchoolJpaEntity e) {
        return new School(
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
