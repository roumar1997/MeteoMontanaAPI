package com.meteomontana.api.infrastructure.persistence;

import com.meteomontana.api.domain.model.SubmissionStatus;
import com.meteomontana.api.infrastructure.persistence.jpa.PendingContributionJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface SpringDataContributionRepository
        extends JpaRepository<PendingContributionJpaEntity, String> {

    List<PendingContributionJpaEntity> findBySubmittedByUidOrderByCreatedAtDesc(String uid);
    List<PendingContributionJpaEntity> findByStatusOrderByCreatedAtDesc(SubmissionStatus status);
    List<PendingContributionJpaEntity> findBySchoolIdOrderByCreatedAtDesc(String schoolId);

    /** Ranking de contribuidores: [uid, count] con ese status, de más a menos.
     *  Una sola query con GROUP BY; el límite va en el Pageable. */
    @Query("select c.submittedByUid, count(c) from PendingContributionJpaEntity c "
        + "where c.status = :status group by c.submittedByUid order by count(c) desc")
    List<Object[]> countBySubmitterWithStatus(SubmissionStatus status, Pageable pageable);
}
