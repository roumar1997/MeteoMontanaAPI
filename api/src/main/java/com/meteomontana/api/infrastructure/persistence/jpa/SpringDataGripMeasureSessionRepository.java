package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataGripMeasureSessionRepository extends JpaRepository<GripMeasureSessionJpaEntity, String> {
    List<GripMeasureSessionJpaEntity> findByUidOrderByCreatedAtDesc(String uid);
    List<GripMeasureSessionJpaEntity> findByUidAndGripTypeIdAndHandOrderByCreatedAtDesc(
            String uid, Integer gripTypeId, String hand);
    void deleteByUid(String uid);
}
