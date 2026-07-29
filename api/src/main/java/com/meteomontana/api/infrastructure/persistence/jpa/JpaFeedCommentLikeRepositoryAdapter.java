package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.port.FeedCommentLikeRepository;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class JpaFeedCommentLikeRepositoryAdapter implements FeedCommentLikeRepository {

    private final SpringDataFeedCommentLikeRepository jpaRepo;

    public JpaFeedCommentLikeRepositoryAdapter(SpringDataFeedCommentLikeRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public boolean exists(String commentId, String uid) {
        return jpaRepo.existsById(new FeedCommentLikeJpaEntity.Key(commentId, uid));
    }

    @Override
    public void add(String commentId, String uid) {
        jpaRepo.save(new FeedCommentLikeJpaEntity(commentId, uid));
    }

    @Override
    public void remove(String commentId, String uid) {
        FeedCommentLikeJpaEntity.Key key = new FeedCommentLikeJpaEntity.Key(commentId, uid);
        if (jpaRepo.existsById(key)) jpaRepo.deleteById(key);
    }

    @Override
    public long countByCommentId(String commentId) {
        return jpaRepo.countByCommentId(commentId);
    }

    @Override
    public Map<String, Long> countByCommentIds(List<String> commentIds) {
        if (commentIds.isEmpty()) return Map.of();
        Map<String, Long> out = new HashMap<>();
        for (Object[] r : jpaRepo.countByCommentIds(commentIds)) {
            out.put((String) r[0], ((Number) r[1]).longValue());
        }
        return out;
    }

    @Override
    public Set<String> likedCommentIds(String uid, List<String> commentIds) {
        if (commentIds.isEmpty()) return Set.of();
        return jpaRepo.likedCommentIds(uid, commentIds).stream().collect(Collectors.toSet());
    }
}
