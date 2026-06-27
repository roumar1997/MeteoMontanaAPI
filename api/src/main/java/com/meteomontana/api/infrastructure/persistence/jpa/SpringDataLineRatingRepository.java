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
}
