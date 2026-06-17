package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataSchoolSubmissionRepository
        extends JpaRepository<SchoolSubmissionJpaEntity, String> {

    List<SchoolSubmissionJpaEntity> findByStatusOrderByCreatedAtAsc(SubmissionStatus status);
    List<SchoolSubmissionJpaEntity> findBySubmittedByUidOrderByCreatedAtDesc(String uid);

    /** Borrado de cuenta. */
    void deleteBySubmittedByUid(String uid);
}
