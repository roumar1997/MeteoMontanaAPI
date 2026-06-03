package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataSchoolPhotoRepository
        extends JpaRepository<SchoolPhotoJpaEntity, String> {

    List<SchoolPhotoJpaEntity> findBySchoolIdOrderByCreatedAtDesc(String schoolId);
}
