package com.meteomontana.api.infrastructure.persistence;

import com.meteomontana.api.domain.model.SubmissionStatus;
import com.meteomontana.api.infrastructure.persistence.jpa.PendingContributionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SpringDataContributionRepository
        extends JpaRepository<PendingContributionJpaEntity, String> {

    List<PendingContributionJpaEntity> findBySubmittedByUidOrderByCreatedAtDesc(String uid);
    List<PendingContributionJpaEntity> findByStatusOrderByCreatedAtDesc(SubmissionStatus status);
    List<PendingContributionJpaEntity> findBySchoolIdOrderByCreatedAtDesc(String schoolId);
}
