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

    /** Último frame del radar en o antes de un instante (para el compuesto:
     *  el dedupe hace que un radar sin cambios no tenga frame en cada ciclo). */
    Optional<RadarFrameEntity> findTopByRadarCodeAndCapturedAtLessThanEqualOrderByCapturedAtDesc(
            String radarCode, LocalDateTime at);

    /** Ciclos con algún frame en una ventana (timeline del compuesto España). */
    @Query("SELECT DISTINCT f.capturedAt FROM RadarFrameEntity f "
            + "WHERE f.capturedAt >= :from AND f.capturedAt < :to ORDER BY f.capturedAt ASC")
    List<LocalDateTime> findDistinctCapturedAtBetween(LocalDateTime from, LocalDateTime to);

    @Modifying
    @Query("DELETE FROM RadarFrameEntity f WHERE f.capturedAt < :cutoff")
    int deleteOlderThan(LocalDateTime cutoff);
}
