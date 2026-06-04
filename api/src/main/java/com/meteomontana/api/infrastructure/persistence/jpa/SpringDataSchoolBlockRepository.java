package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataSchoolBlockRepository
        extends JpaRepository<SchoolBlockJpaEntity, String> {

    List<SchoolBlockJpaEntity> findBySchoolIdOrderByCreatedAtAsc(String schoolId);
}
