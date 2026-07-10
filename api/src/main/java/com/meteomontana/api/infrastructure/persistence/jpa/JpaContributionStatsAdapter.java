package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.SubmissionStatus;
import com.meteomontana.api.domain.port.ContributionStatsRepository;
import com.meteomontana.api.infrastructure.persistence.SpringDataContributionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JpaContributionStatsAdapter implements ContributionStatsRepository {

    private final SpringDataContributionRepository repo;

    public JpaContributionStatsAdapter(SpringDataContributionRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<ContributorCount> topContributors(SubmissionStatus status, int limit) {
        return repo.countBySubmitterWithStatus(status, PageRequest.of(0, limit)).stream()
                .map(row -> new ContributorCount((String) row[0], (Long) row[1]))
                .toList();
    }
}
