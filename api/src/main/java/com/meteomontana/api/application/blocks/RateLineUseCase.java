package com.meteomontana.api.application.blocks;

import com.meteomontana.api.infrastructure.persistence.jpa.LineRatingJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataLineRatingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RateLineUseCase {

    public record RatingResult(float avgStars, long ratingCount, int myStars) {}

    private final SpringDataLineRatingRepository repo;

    public RateLineUseCase(SpringDataLineRatingRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public RatingResult rate(String uid, String lineId, int stars) {
        if (stars < 1 || stars > 5) throw new IllegalArgumentException("stars must be 1-5");

        var existing = repo.findByUidAndLineId(uid, lineId);
        if (existing.isPresent()) {
            existing.get().setStars(stars);
            repo.save(existing.get());
        } else {
            repo.save(new LineRatingJpaEntity(
                    UUID.randomUUID().toString(), uid, lineId, stars, LocalDateTime.now()
            ));
        }

        Double avg = repo.avgStarsByLineId(lineId);
        long count = repo.countByLineId(lineId);
        return new RatingResult(avg == null ? stars : avg.floatValue(), count, stars);
    }

    @Transactional
    public RatingResult unrate(String uid, String lineId) {
        repo.deleteByUidAndLineId(uid, lineId);
        Double avg = repo.avgStarsByLineId(lineId);
        long count = repo.countByLineId(lineId);
        return new RatingResult(avg == null ? 0f : avg.floatValue(), count, 0);
    }

    public RatingResult getStats(String uid, String lineId) {
        Double avg = repo.avgStarsByLineId(lineId);
        long count = repo.countByLineId(lineId);
        int myStars = repo.findByUidAndLineId(uid, lineId)
                .map(LineRatingJpaEntity::getStars).orElse(0);
        return new RatingResult(avg == null ? 0f : avg.floatValue(), count, myStars);
    }
}
