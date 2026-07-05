package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SpringDataUserRepository
        extends JpaRepository<UserJpaEntity, String> {

    Optional<UserJpaEntity> findByUsernameIgnoreCase(String username);
    List<UserJpaEntity> findAllByFcmTokenIsNotNull();

    /** Cuenta admins en BD (en vez de cargar todos los usuarios en memoria). */
    @Query("select count(u) from UserJpaEntity u where u.isAdmin = true")
    long countAdmins();

    /** Admins (para avisarles por push de denuncias/propuestas nuevas). */
    List<UserJpaEntity> findByIsAdminTrue();

    /** ¿Está baneado este uid? (proyección ligera para el filtro de auth). */
    @Query("select coalesce(u.banned, false) from UserJpaEntity u where u.uid = :uid")
    Boolean isBanned(String uid);

    /** Búsqueda acotada en BD (LIKE + LIMIT 100): evita cargar toda la tabla en
     *  memoria en el endpoint público de búsqueda (riesgo de DoS). */
    List<UserJpaEntity> findTop100ByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
            String username, String displayName);
}
