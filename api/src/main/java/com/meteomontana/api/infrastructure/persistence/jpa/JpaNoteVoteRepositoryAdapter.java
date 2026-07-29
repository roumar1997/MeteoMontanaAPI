package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.port.NoteVoteRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class JpaNoteVoteRepositoryAdapter implements NoteVoteRepository {

    private final SpringDataNoteVoteRepository jpaRepo;

    public JpaNoteVoteRepositoryAdapter(SpringDataNoteVoteRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public int voteOf(String noteId, String uid) {
        return jpaRepo.findByNoteIdAndUid(noteId, uid)
                .map(NoteVoteJpaEntity::getVoteValue).orElse(0);
    }

    @Override
    public Map<String, Integer> votesOf(String uid, List<String> noteIds) {
        if (noteIds.isEmpty()) return Map.of();
        return jpaRepo.findByUidAndNoteIdIn(uid, noteIds).stream()
                .collect(Collectors.toMap(NoteVoteJpaEntity::getNoteId,
                                          NoteVoteJpaEntity::getVoteValue));
    }

    @Override
    public void setVote(String noteId, String uid, int value) {
        NoteVoteJpaEntity existing = jpaRepo.findByNoteIdAndUid(noteId, uid).orElse(null);
        if (existing != null) {
            existing.setVoteValue(value);
            jpaRepo.save(existing);
        } else {
            jpaRepo.save(new NoteVoteJpaEntity(noteId, uid, value));
        }
    }

    @Override
    public void removeVote(String noteId, String uid) {
        jpaRepo.findByNoteIdAndUid(noteId, uid).ifPresent(jpaRepo::delete);
    }

    @Override
    public int adjustVoteCounts(String noteId, int deltaUp, int deltaDown) {
        return jpaRepo.adjustCounts(noteId, deltaUp, deltaDown);
    }
}
