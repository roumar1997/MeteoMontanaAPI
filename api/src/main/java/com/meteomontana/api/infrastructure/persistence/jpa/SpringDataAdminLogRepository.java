package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SpringDataAdminLogRepository
        extends JpaRepository<AdminLogJpaEntity, String> {

    @Query("SELECT a FROM AdminLogJpaEntity a ORDER BY a.createdAt DESC")
    List<AdminLogJpaEntity> findRecent(Pageable pageable);
}
