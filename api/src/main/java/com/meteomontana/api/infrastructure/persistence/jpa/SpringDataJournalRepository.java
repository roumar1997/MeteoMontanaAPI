package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SpringDataJournalRepository
        extends JpaRepository<JournalSessionJpaEntity, String> {

    List<JournalSessionJpaEntity> findByUidOrderBySessionDateDesc(String uid);

    /** Borrado de cuenta. */
    void deleteByUid(String uid);

    /**
     * Propaga el grado nuevo de una vía a TODAS las entradas de diario que la
     * tienen marcada (enganche por lineId estable). Así "todo en vivo": si una
     * vía pasa de 6a a 6b, el perfil de todos refleja 6b (y las stats máximas).
     */
    @Modifying
    @Transactional
    @Query("update JournalSessionJpaEntity j set j.grade = :grade where j.lineId = :lineId")
    int updateGradeByLineId(@Param("lineId") String lineId, @Param("grade") String grade);

    /** Propaga la modalidad nueva (bloque/vía) a las entradas de un conjunto de vías. */
    @Modifying
    @Transactional
    @Query("update JournalSessionJpaEntity j set j.discipline = :discipline where j.lineId in :lineIds")
    int updateDisciplineByLineIds(@Param("lineIds") List<String> lineIds, @Param("discipline") String discipline);
}
