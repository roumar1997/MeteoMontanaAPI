package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface SpringDataMeetupRepository extends JpaRepository<MeetupJpaEntity, String> {

    /** Quedadas no caducadas, ordenadas por el primer día más próximo. */
    @Query("SELECT m FROM MeetupJpaEntity m WHERE m.expiresAt > :now ORDER BY m.lastDay ASC")
    List<MeetupJpaEntity> findActiveOrderByLastDay(LocalDateTime now);

    /** Para el @Scheduled de caducidad. */
    @Query("SELECT m FROM MeetupJpaEntity m WHERE m.expiresAt <= :now")
    List<MeetupJpaEntity> findExpired(LocalDateTime now);

    /** Quedadas de un creador. */
    List<MeetupJpaEntity> findByCreatorUid(String creatorUid);
}
