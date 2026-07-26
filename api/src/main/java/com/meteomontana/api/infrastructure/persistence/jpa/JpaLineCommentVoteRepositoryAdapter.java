package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.port.LineCommentVoteRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class JpaLineCommentVoteRepositoryAdapter implements LineCommentVoteRepository {

    private final SpringDataLineCommentVoteRepository jpaRepo;

    public JpaLineCommentVoteRepositoryAdapter(SpringDataLineCommentVoteRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public int voteOf(String commentId, String uid) {
        return jpaRepo.findByCommentIdAndUid(commentId, uid)
                .map(LineCommentVoteJpaEntity::getVoteValue).orElse(0);
    }

    @Override
    public Map<String, Integer> votesOf(String uid, List<String> commentIds) {
        if (commentIds.isEmpty()) return Map.of();
        return jpaRepo.findByUidAndCommentIdIn(uid, commentIds).stream()
                .collect(Collectors.toMap(LineCommentVoteJpaEntity::getCommentId,
                                          LineCommentVoteJpaEntity::getVoteValue));
    }

    @Override
    public void setVote(String commentId, String uid, int value) {
        LineCommentVoteJpaEntity existing = jpaRepo.findByCommentIdAndUid(commentId, uid).orElse(null);
        if (existing != null) {
            existing.setVoteValue(value);
            jpaRepo.save(existing);
        } else {
            jpaRepo.save(new LineCommentVoteJpaEntity(commentId, uid, value));
        }
    }

    @Override
    public void removeVote(String commentId, String uid) {
        jpaRepo.findByCommentIdAndUid(commentId, uid).ifPresent(jpaRepo::delete);
    }
}
