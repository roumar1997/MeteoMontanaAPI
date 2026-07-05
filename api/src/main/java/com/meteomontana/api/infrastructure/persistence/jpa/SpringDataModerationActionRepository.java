package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataModerationActionRepository
        extends JpaRepository<ModerationActionJpaEntity, String> {

    /** Historial de acciones sobre un usuario, recientes primero. */
    List<ModerationActionJpaEntity> findByTargetUidOrderByCreatedAtDesc(String targetUid);
}
