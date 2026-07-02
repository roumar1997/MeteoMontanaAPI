package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataGripMaxRecordRepository extends JpaRepository<GripMaxRecordJpaEntity, String> {
    List<GripMaxRecordJpaEntity> findByUid(String uid);
    Optional<GripMaxRecordJpaEntity> findByUidAndGripTypeIdAndHand(String uid, Integer gripTypeId, String hand);
    void deleteByUid(String uid);
}
