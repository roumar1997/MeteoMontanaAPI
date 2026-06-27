package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.MeetupReport;
import com.meteomontana.api.domain.port.MeetupReportRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MeetupReportRepositoryAdapter implements MeetupReportRepository {

    private final MeetupReportJpaRepository jpa;

    public MeetupReportRepositoryAdapter(MeetupReportJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public MeetupReport save(MeetupReport report) {
        String id = report.getId() != null ? report.getId() : UUID.randomUUID().toString();
        MeetupReportJpaEntity entity = new MeetupReportJpaEntity(
                id, report.getMeetupId(), report.getReporterUid(), report.getReportedUid(),
                report.getReason(), report.getContext(), report.getStatus(),
                report.getResolvedBy(), report.getResolvedAt(), report.getCreatedAt());
        return jpa.save(entity).toDomain();
    }

    @Override
    public Optional<MeetupReport> findById(String id) {
        return jpa.findById(id).map(MeetupReportJpaEntity::toDomain);
    }

    @Override
    public List<MeetupReport> findPending() {
        return jpa.findByStatusOrderByCreatedAtAsc(MeetupReport.Status.PENDING)
                  .stream().map(MeetupReportJpaEntity::toDomain).toList();
    }

    @Override
    public boolean existsByReporterAndMeetup(String reporterUid, String meetupId) {
        return jpa.existsByReporterUidAndMeetupId(reporterUid, meetupId);
    }
}
