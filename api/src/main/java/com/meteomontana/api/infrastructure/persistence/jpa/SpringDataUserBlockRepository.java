package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataUserBlockRepository
        extends JpaRepository<UserBlockJpaEntity, UserBlockJpaEntity.Key> {
    List<UserBlockJpaEntity> findByBlockerUid(String blockerUid);
    boolean existsByBlockerUidAndBlockedUid(String blockerUid, String blockedUid);
}
