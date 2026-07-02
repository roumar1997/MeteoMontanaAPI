package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataGripWorkoutRepository extends JpaRepository<GripWorkoutJpaEntity, String> {
    List<GripWorkoutJpaEntity> findByUidOrderByUpdatedAtDesc(String uid);
    void deleteByUid(String uid);
}
