package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.port.LineRatingRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class JpaLineRatingRepositoryAdapter implements LineRatingRepository {

    private final SpringDataLineRatingRepository jpaRepo;

    public JpaLineRatingRepositoryAdapter(SpringDataLineRatingRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public int starsOf(String uid, String lineId) {
        return jpaRepo.findByUidAndLineId(uid, lineId)
                .map(LineRatingJpaEntity::getStars).orElse(0);
    }

    @Override
    public void setStars(String uid, String lineId, int stars) {
        var existing = jpaRepo.findByUidAndLineId(uid, lineId);
        if (existing.isPresent()) {
            existing.get().setStars(stars);
            jpaRepo.save(existing.get());
        } else {
            jpaRepo.save(new LineRatingJpaEntity(
                    UUID.randomUUID().toString(), uid, lineId, stars, LocalDateTime.now()));
        }
    }

    @Override
    public void removeRating(String uid, String lineId) {
        jpaRepo.deleteByUidAndLineId(uid, lineId);
    }

    @Override
    public Double avgStars(String lineId) { return jpaRepo.avgStarsByLineId(lineId); }

    @Override
    public long countRatings(String lineId) { return jpaRepo.countByLineId(lineId); }

    @Override
    public Map<String, Double> avgStarsByLineIds(Collection<String> lineIds) {
        if (lineIds.isEmpty()) return Map.of();
        Map<String, Double> out = new HashMap<>();
        for (Object[] row : jpaRepo.avgStarsByLineIds(lineIds)) {
            out.put((String) row[0], ((Number) row[1]).doubleValue());
        }
        return out;
    }

    @Override
    public Map<String, Integer> myStarsByLineIds(String uid, Collection<String> lineIds) {
        if (lineIds.isEmpty()) return Map.of();
        return jpaRepo.findByUidAndLineIdIn(uid, lineIds).stream()
                .collect(Collectors.toMap(LineRatingJpaEntity::getLineId,
                                          LineRatingJpaEntity::getStars));
    }
}
