package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.FeedComment;
import com.meteomontana.api.domain.port.FeedCommentRepository;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class JpaFeedCommentRepositoryAdapter implements FeedCommentRepository {

    private final SpringDataFeedCommentRepository jpaRepo;

    public JpaFeedCommentRepositoryAdapter(SpringDataFeedCommentRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public List<FeedComment> findByPostId(long postId) {
        return jpaRepo.findByPostIdOrderByCreatedAtAsc(postId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<FeedComment> findById(String id) {
        return jpaRepo.findById(id).map(this::toDomain);
    }

    @Override
    public FeedComment create(FeedComment c) {
        FeedCommentJpaEntity saved = jpaRepo.save(new FeedCommentJpaEntity(
                c.id(), c.postId(), c.uid(), c.author(), c.text(), c.parentId()));
        return toDomain(saved);
    }

    @Override
    public void deleteById(String id) { jpaRepo.deleteById(id); }

    @Override
    public Map<Long, Long> countByPostIds(List<Long> postIds) {
        if (postIds.isEmpty()) return Map.of();
        Map<Long, Long> out = new HashMap<>();
        for (Object[] r : jpaRepo.countByPostIds(postIds)) {
            out.put(((Number) r[0]).longValue(), ((Number) r[1]).longValue());
        }
        return out;
    }

    private FeedComment toDomain(FeedCommentJpaEntity e) {
        return new FeedComment(e.getId(), e.getPostId(), e.getUid(), e.getAuthor(),
                e.getText(), e.getParentId(), e.getCreatedAt());
    }
}
