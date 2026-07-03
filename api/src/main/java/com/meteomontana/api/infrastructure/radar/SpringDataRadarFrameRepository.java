package com.meteomontana.api.infrastructure.radar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SpringDataRadarFrameRepository extends JpaRepository<RadarFrameEntity, Long> {

    Optional<RadarFrameEntity> findTopByRadarCodeOrderByCapturedAtDesc(String radarCode);

    List<RadarFrameEntity> findByRadarCodeAndCapturedAtAfterOrderByCapturedAtAsc(
            String radarCode, LocalDateTime after);

    Optional<RadarFrameEntity> findByRadarCodeAndCapturedAt(String radarCode, LocalDateTime capturedAt);

    @Modifying
    @Query("DELETE FROM RadarFrameEntity f WHERE f.capturedAt < :cutoff")
    int deleteOlderThan(LocalDateTime cutoff);
}
