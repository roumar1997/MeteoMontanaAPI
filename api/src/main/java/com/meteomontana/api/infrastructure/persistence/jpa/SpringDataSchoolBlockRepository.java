package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpringDataSchoolBlockRepository
        extends JpaRepository<SchoolBlockJpaEntity, String> {

    /**
     * Trae los bloques de una escuela CON sus líneas en UNA sola consulta
     * (LEFT JOIN FETCH) en vez de N+1 (una query por bloque para sus líneas).
     * `distinct` evita filas de bloque duplicadas por el join 1→N; el orden de
     * las líneas lo mantiene el @OrderBy("sortOrder ASC") de la colección.
     */
    @Query("select distinct b from SchoolBlockJpaEntity b " +
           "left join fetch b.lines " +
           "where b.schoolId = :schoolId " +
           "order by b.createdAt asc")
    List<SchoolBlockJpaEntity> findBySchoolIdOrderByCreatedAtAsc(@Param("schoolId") String schoolId);

    /** Bloque que contiene una vía concreta (para los enlaces compartidos). */
    @org.springframework.data.jpa.repository.Query(
        "SELECT DISTINCT b FROM SchoolBlockJpaEntity b LEFT JOIN FETCH b.lines WHERE EXISTS "
        + "(SELECT 1 FROM BlockLineJpaEntity l WHERE l.block = b AND l.id = :lineId)")
    java.util.Optional<SchoolBlockJpaEntity> findByLineId(@org.springframework.data.repository.query.Param("lineId") String lineId);
}
