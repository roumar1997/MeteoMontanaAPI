package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataForecastCacheRepository
        extends JpaRepository<ForecastCacheJpaEntity, String> {
}
