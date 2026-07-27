package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.PendingContribution;
import com.meteomontana.api.domain.port.PendingContributionRepository;
import com.meteomontana.api.infrastructure.persistence.SpringDataContributionRepository;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPendingContributionRepositoryAdapter implements PendingContributionRepository {

    private final SpringDataContributionRepository jpaRepo;

    public JpaPendingContributionRepositoryAdapter(SpringDataContributionRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public String save(PendingContribution contribution) {
        return jpaRepo.save(PendingContributionJpaEntity.from(contribution)).getId();
    }

    @Override
    public java.util.List<PendingContribution> findBySubmitter(String uid) {
        return jpaRepo.findBySubmittedByUidOrderByCreatedAtDesc(uid)
                .stream().map(PendingContributionJpaEntity::toDomain).toList();
    }

    @Override
    public java.util.List<PendingContribution> findPending() {
        return jpaRepo.findByStatusOrderByCreatedAtDesc(
                        com.meteomontana.api.domain.model.SubmissionStatus.PENDING)
                .stream().map(PendingContributionJpaEntity::toDomain).toList();
    }
}
