package com.meteomontana.api.application.blocks;

import com.meteomontana.api.domain.port.LineRatingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class RateLineUseCase {

    public record RatingResult(float avgStars, long ratingCount, int myStars) {}

    private final LineRatingRepository repo;

    public RateLineUseCase(LineRatingRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public RatingResult rate(String uid, String lineId, int stars) {
        if (stars < 1 || stars > 5) throw new IllegalArgumentException("stars must be 1-5");

        repo.setStars(uid, lineId, stars);

        Double avg = repo.avgStars(lineId);
        long count = repo.countRatings(lineId);
        return new RatingResult(avg == null ? stars : avg.floatValue(), count, stars);
    }

    @Transactional
    public RatingResult unrate(String uid, String lineId) {
        repo.removeRating(uid, lineId);
        Double avg = repo.avgStars(lineId);
        long count = repo.countRatings(lineId);
        return new RatingResult(avg == null ? 0f : avg.floatValue(), count, 0);
    }

    public RatingResult getStats(String uid, String lineId) {
        Double avg = repo.avgStars(lineId);
        long count = repo.countRatings(lineId);
        int myStars = repo.starsOf(uid, lineId);
        return new RatingResult(avg == null ? 0f : avg.floatValue(), count, myStars);
    }
}
