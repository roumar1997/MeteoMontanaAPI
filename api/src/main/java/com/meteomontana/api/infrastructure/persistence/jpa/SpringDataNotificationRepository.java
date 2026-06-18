package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SpringDataNotificationRepository
        extends JpaRepository<NotificationJpaEntity, String> {

    List<NotificationJpaEntity> findByUidOrderByCreatedAtDesc(String uid, Pageable pageable);

    long countByUidAndReadAtIsNull(String uid);

    @Modifying
    @Query("UPDATE NotificationJpaEntity n SET n.readAt = :now WHERE n.uid = :uid AND n.readAt IS NULL")
    void markAllAsRead(@Param("uid") String uid, @Param("now") LocalDateTime now);

    /** Borrado de cuenta / borrar todas. */
    void deleteByUid(String uid);

    /** Borra una notificación solo si es del usuario (devuelve nº borradas). */
    long deleteByIdAndUid(String id, String uid);
}
