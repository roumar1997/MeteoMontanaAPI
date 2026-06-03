package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataUserRepository
        extends JpaRepository<UserJpaEntity, String> {

    Optional<UserJpaEntity> findByUsernameIgnoreCase(String username);
    List<UserJpaEntity> findAllByFcmTokenIsNotNull();
}
