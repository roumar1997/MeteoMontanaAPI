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
}
