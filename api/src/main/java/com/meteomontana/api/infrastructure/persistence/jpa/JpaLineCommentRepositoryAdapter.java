package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.LineComment;
import com.meteomontana.api.domain.port.LineCommentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaLineCommentRepositoryAdapter implements LineCommentRepository {

    private final SpringDataLineCommentRepository jpaRepo;
    private final SpringDataLineCommentVoteRepository voteRepo;

    public JpaLineCommentRepositoryAdapter(SpringDataLineCommentRepository jpaRepo,
                                           SpringDataLineCommentVoteRepository voteRepo) {
        this.jpaRepo = jpaRepo;
        this.voteRepo = voteRepo;
    }

    @Override
    public List<LineComment> findByBlockId(String blockId) {
        return jpaRepo.findByBlockId(blockId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<LineComment> findById(String id) {
        return jpaRepo.findById(id).map(this::toDomain);
    }

    @Override
    public LineComment create(LineComment c) {
        LineCommentJpaEntity saved = jpaRepo.save(new LineCommentJpaEntity(
                c.id(), c.blockId(), c.lineId(), c.uid(), c.author(), c.text()));
        return toDomain(saved);
    }

    @Override
    public void deleteById(String id) { jpaRepo.deleteById(id); }

    @Override
    public int adjustVoteCounts(String commentId, int deltaUp, int deltaDown) {
        // El UPDATE atómico vive en el repo de votos por razones históricas de
        // JPQL; el puerto lo expone donde tiene sentido (los contadores son
        // del comentario).
        return voteRepo.adjustCounts(commentId, deltaUp, deltaDown);
    }

    private LineComment toDomain(LineCommentJpaEntity e) {
        return new LineComment(e.getId(), e.getBlockId(), e.getLineId(), e.getUid(),
                e.getAuthor(), e.getText(), e.getUpvotesCount(), e.getDownvotesCount(),
                e.getCreatedAt());
    }
}
