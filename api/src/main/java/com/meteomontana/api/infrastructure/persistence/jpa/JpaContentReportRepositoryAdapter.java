package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.ContentReport;
import com.meteomontana.api.domain.port.ContentReportRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class JpaContentReportRepositoryAdapter implements ContentReportRepository {

    private final SpringDataContentReportRepository jpaRepo;

    public JpaContentReportRepositoryAdapter(SpringDataContentReportRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public List<ContentReport> findByStatus(String status) {
        return jpaRepo.findByStatusOrderByCreatedAtDesc(status).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<ContentReport> findById(String id) {
        return jpaRepo.findById(id).map(this::toDomain);
    }

    @Override
    public boolean alreadyReported(String reporterUid, String targetType, String targetId) {
        return jpaRepo.existsByReporterUidAndTargetTypeAndTargetId(reporterUid, targetType, targetId);
    }

    @Override
    public long countByAuthor(String authorUid) { return jpaRepo.countByAuthorUid(authorUid); }

    @Override
    public List<ContentReport> findByAuthor(String authorUid) {
        return jpaRepo.findByAuthorUidOrderByCreatedAtDesc(authorUid).stream().map(this::toDomain).toList();
    }

    @Override
    public ContentReport create(ContentReport r) {
        return toDomain(jpaRepo.save(new ContentReportJpaEntity(
                r.id(), r.reporterUid(), r.targetType(), r.targetId(),
                r.reason(), r.snapshot(), r.authorUid())));
    }

    @Override
    public void resolve(String id, String status, String resolution, LocalDateTime resolvedAt) {
        jpaRepo.findById(id).ifPresent(e -> {
            e.resolve(resolution);
            jpaRepo.save(e);
        });
    }

    private ContentReport toDomain(ContentReportJpaEntity e) {
        return new ContentReport(e.getId(), e.getReporterUid(), e.getTargetType(),
                e.getTargetId(), e.getReason(), e.getSnapshot(), e.getAuthorUid(),
                e.getStatus(), e.getResolution(), e.getCreatedAt(), e.getResolvedAt());
    }
}
