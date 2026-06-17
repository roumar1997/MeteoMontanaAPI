package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataUserRepository
        extends JpaRepository<UserJpaEntity, String> {

    Optional<UserJpaEntity> findByUsernameIgnoreCase(String username);
    List<UserJpaEntity> findAllByFcmTokenIsNotNull();

    /** Búsqueda acotada en BD (LIKE + LIMIT 100): evita cargar toda la tabla en
     *  memoria en el endpoint público de búsqueda (riesgo de DoS). */
    List<UserJpaEntity> findTop100ByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
            String username, String displayName);
}
