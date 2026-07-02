package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataGripWorkoutSetRepository extends JpaRepository<GripWorkoutSetJpaEntity, String> {
    List<GripWorkoutSetJpaEntity> findByWorkoutIdOrderBySortOrder(String workoutId);
    void deleteByWorkoutId(String workoutId);
}
