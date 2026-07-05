package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.MeetupReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetupReportJpaRepository extends JpaRepository<MeetupReportJpaEntity, String> {
    List<MeetupReportJpaEntity> findByStatusOrderByCreatedAtAsc(MeetupReport.Status status);
    boolean existsByReporterUidAndMeetupId(String reporterUid, String meetupId);

    /** Nº de denuncias recibidas por un usuario (como organizador denunciado). */
    long countByReportedUid(String reportedUid);
}
