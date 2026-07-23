package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SpringDataLineRatingRepository extends JpaRepository<LineRatingJpaEntity, String> {

    Optional<LineRatingJpaEntity> findByUidAndLineId(String uid, String lineId);

    void deleteByUidAndLineId(String uid, String lineId);

    @Query("SELECT AVG(r.stars) FROM LineRatingJpaEntity r WHERE r.lineId = :lineId")
    Double avgStarsByLineId(@Param("lineId") String lineId);

    @Query("SELECT COUNT(r) FROM LineRatingJpaEntity r WHERE r.lineId = :lineId")
    long countByLineId(@Param("lineId") String lineId);

    /** Medias por vía EN LOTE (mata el N+1 de listar bloques: 2 queries por vía → 2 por escuela). */
    @Query("SELECT r.lineId, AVG(r.stars) FROM LineRatingJpaEntity r WHERE r.lineId IN :ids GROUP BY r.lineId")
    java.util.List<Object[]> avgStarsByLineIds(@Param("ids") java.util.Collection<String> ids);

    /** Votos del usuario para varias vías EN LOTE. */
    java.util.List<LineRatingJpaEntity> findByUidAndLineIdIn(String uid, java.util.Collection<String> lineIds);
}
