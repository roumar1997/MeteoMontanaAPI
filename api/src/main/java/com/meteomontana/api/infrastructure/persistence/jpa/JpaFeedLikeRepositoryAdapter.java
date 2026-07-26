package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.port.FeedLikeRepository;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class JpaFeedLikeRepositoryAdapter implements FeedLikeRepository {

    private final SpringDataFeedLikeRepository jpaRepo;

    public JpaFeedLikeRepositoryAdapter(SpringDataFeedLikeRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public boolean exists(long postId, String uid) {
        return jpaRepo.existsById(new FeedLikeJpaEntity.Key(postId, uid));
    }

    @Override
    public void add(long postId, String uid) {
        jpaRepo.save(new FeedLikeJpaEntity(postId, uid));
    }

    @Override
    public void remove(long postId, String uid) {
        FeedLikeJpaEntity.Key key = new FeedLikeJpaEntity.Key(postId, uid);
        if (jpaRepo.existsById(key)) jpaRepo.deleteById(key);
    }

    @Override
    public Map<Long, Long> countByPostIds(List<Long> postIds) {
        if (postIds.isEmpty()) return Map.of();
        Map<Long, Long> out = new HashMap<>();
        for (Object[] r : jpaRepo.countByPostIds(postIds)) {
            out.put(((Number) r[0]).longValue(), ((Number) r[1]).longValue());
        }
        return out;
    }

    @Override
    public Set<Long> likedPostIds(String uid, List<Long> postIds) {
        if (postIds.isEmpty()) return Set.of();
        return jpaRepo.likedPostIds(uid, postIds).stream().collect(Collectors.toSet());
    }
}
