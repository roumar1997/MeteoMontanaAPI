package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.CommunityVotes.GradeVote;
import com.meteomontana.api.domain.model.CommunityVotes.OrientationVote;
import com.meteomontana.api.domain.port.CommunityVoteRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.CommunityVoteJpaEntities.GradeVoteJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.CommunityVoteJpaEntities.OrientationVoteJpaEntity;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
public class JpaCommunityVoteRepositoryAdapter implements CommunityVoteRepository {

    public interface SpringDataOrientationVoteRepository
            extends JpaRepository<OrientationVoteJpaEntity, String> {
        List<OrientationVoteJpaEntity> findByBlockId(String blockId);
    }

    public interface SpringDataGradeVoteRepository
            extends JpaRepository<GradeVoteJpaEntity, String> {
        List<GradeVoteJpaEntity> findByLineId(String lineId);
    }

    private final SpringDataOrientationVoteRepository orientationRepo;
    private final SpringDataGradeVoteRepository gradeRepo;
    private final EntityManager em;

    public JpaCommunityVoteRepositoryAdapter(SpringDataOrientationVoteRepository orientationRepo,
                                             SpringDataGradeVoteRepository gradeRepo,
                                             EntityManager em) {
        this.orientationRepo = orientationRepo;
        this.gradeRepo = gradeRepo;
        this.em = em;
    }

    @Override
    public List<OrientationVote> findOrientationVotes(String blockId) {
        return orientationRepo.findByBlockId(blockId).stream()
                .map(e -> new OrientationVote(e.getBlockId(), e.getPhotoIndex(),
                        e.getVoterUid(), e.getAspect()))
                .toList();
    }

    @Override
    @Transactional
    public void upsertOrientationVote(OrientationVote vote) {
        OrientationVoteJpaEntity existing = orientationRepo.findByBlockId(vote.blockId()).stream()
                .filter(e -> e.getVoterUid().equals(vote.voterUid())
                        && Objects.equals(e.getPhotoIndex(), vote.photoIndex()))
                .findFirst().orElse(null);
        if (existing != null) {
            existing.setAspect(vote.aspect());
            orientationRepo.save(existing);
        } else {
            orientationRepo.save(new OrientationVoteJpaEntity(
                    UUID.randomUUID().toString(), vote.blockId(), vote.photoIndex(),
                    vote.voterUid(), vote.aspect(), LocalDateTime.now()));
        }
    }

    @Override
    public List<GradeVote> findGradeVotes(String lineId) {
        return gradeRepo.findByLineId(lineId).stream()
                .map(e -> new GradeVote(e.getLineId(), e.getVoterUid(), e.getGrade()))
                .toList();
    }

    @Override
    @Transactional
    public void upsertGradeVote(GradeVote vote) {
        GradeVoteJpaEntity existing = gradeRepo.findByLineId(vote.lineId()).stream()
                .filter(e -> e.getVoterUid().equals(vote.voterUid()))
                .findFirst().orElse(null);
        if (existing != null) {
            existing.setGrade(vote.grade());
            gradeRepo.save(existing);
        } else {
            gradeRepo.save(new GradeVoteJpaEntity(
                    UUID.randomUUID().toString(), vote.lineId(), vote.voterUid(),
                    vote.grade(), LocalDateTime.now()));
        }
    }

    /**
     * El consenso se propaga en UNA transacción a la vía Y a los diarios de
     * todos los usuarios con esa vía (decisión: el perfil también cambia).
     */
    @Override
    @Transactional
    public void applyDisplayedGrade(String lineId, String displayedGrade) {
        em.createQuery("UPDATE BlockLineJpaEntity l SET l.grade = :g WHERE l.id = :id")
                .setParameter("g", displayedGrade).setParameter("id", lineId)
                .executeUpdate();
        em.createQuery("UPDATE JournalSessionJpaEntity j SET j.grade = :g WHERE j.lineId = :id")
                .setParameter("g", displayedGrade).setParameter("id", lineId)
                .executeUpdate();
    }
}
