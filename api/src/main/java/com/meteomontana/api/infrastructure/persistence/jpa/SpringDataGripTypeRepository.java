package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataGripTypeRepository extends JpaRepository<GripTypeJpaEntity, Integer> {
}
