package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataMeetupAlertRepository extends JpaRepository<MeetupAlertJpaEntity, String> {

    Optional<MeetupAlertJpaEntity> findByUidAndSchoolId(String uid, String schoolId);

    void deleteByUidAndSchoolId(String uid, String schoolId);

    /** Alertas para esta escuela + alertas globales (school_id IS NULL). */
    @Query("SELECT a FROM MeetupAlertJpaEntity a WHERE a.schoolId = :schoolId OR a.schoolId IS NULL")
    List<MeetupAlertJpaEntity> findBySchoolIdOrGlobal(@Param("schoolId") String schoolId);
}
