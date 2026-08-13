package com.meteomontana.api.infrastructure.persistence.jpa;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataApproachRepository extends JpaRepository<ApproachJpaEntity, String> {
    List<ApproachJpaEntity> findBySchoolId(String schoolId);
}
