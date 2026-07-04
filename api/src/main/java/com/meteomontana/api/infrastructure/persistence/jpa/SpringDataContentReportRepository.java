package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataContentReportRepository extends JpaRepository<ContentReportJpaEntity, String> {
    List<ContentReportJpaEntity> findByStatusOrderByCreatedAtDesc(String status);
    boolean existsByReporterUidAndTargetTypeAndTargetId(String reporterUid, String targetType, String targetId);
}
