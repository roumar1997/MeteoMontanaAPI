package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.SchoolSubmission;
import com.meteomontana.api.domain.model.SubmissionStatus;
import com.meteomontana.api.domain.port.SchoolSubmissionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaSchoolSubmissionRepositoryAdapter implements SchoolSubmissionRepository {

    private final SpringDataSchoolSubmissionRepository jpaRepo;

    public JpaSchoolSubmissionRepositoryAdapter(SpringDataSchoolSubmissionRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public SchoolSubmission save(SchoolSubmission s) {
        SchoolSubmissionJpaEntity e = new SchoolSubmissionJpaEntity(
                s.getId(), s.getProposedName(), s.getProposedRegion(),
                s.getProposedStyle(), s.getProposedRockType(),
                s.getProposedLat(), s.getProposedLon(),
                s.getProposedLocation(), s.getProposedSource(), s.getNotes(),
                s.getStatus(), s.getSubmittedByUid(),
                s.getReviewedByUid(), s.getReviewReason(), s.getCreatedSchoolId(),
                s.getCreatedAt(), s.getReviewedAt()
        );
        return toDomain(jpaRepo.save(e));
    }

    @Override
    public Optional<SchoolSubmission> findById(String id) {
        return jpaRepo.findById(id).map(this::toDomain);
    }

    @Override
    public List<SchoolSubmission> findByStatus(SubmissionStatus status) {
        return jpaRepo.findByStatusOrderByCreatedAtAsc(status).stream()
                .map(this::toDomain).toList();
    }

    @Override
    public List<SchoolSubmission> findBySubmittedByUid(String uid) {
        return jpaRepo.findBySubmittedByUidOrderByCreatedAtDesc(uid).stream()
                .map(this::toDomain).toList();
    }

    private SchoolSubmission toDomain(SchoolSubmissionJpaEntity e) {
        return new SchoolSubmission(
                e.getId(), e.getProposedName(), e.getProposedRegion(),
                e.getProposedStyle(), e.getProposedRockType(),
                e.getProposedLat(), e.getProposedLon(),
                e.getProposedLocation(), e.getProposedSource(), e.getNotes(),
                e.getStatus(), e.getSubmittedByUid(),
                e.getReviewedByUid(), e.getReviewReason(), e.getCreatedSchoolId(),
                e.getCreatedAt(), e.getReviewedAt()
        );
    }
}
