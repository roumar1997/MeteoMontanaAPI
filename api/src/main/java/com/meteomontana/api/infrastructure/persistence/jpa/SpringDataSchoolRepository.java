package com.meteomontana.api.infrastructure.persistence.jpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataSchoolRepository
        extends JpaRepository<SchoolJpaEntity, String> {

}
