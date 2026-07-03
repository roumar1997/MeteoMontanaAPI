package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataNoteVoteRepository extends JpaRepository<NoteVoteJpaEntity, String> {

    Optional<NoteVoteJpaEntity> findByNoteIdAndUid(String noteId, String uid);

    List<NoteVoteJpaEntity> findByUidAndNoteIdIn(String uid, List<String> noteIds);

    /** Ajuste atómico de los contadores agregados de la nota. */
    @Modifying
    @Query("UPDATE NoteJpaEntity n SET n.upvotesCount = n.upvotesCount + :dUp, "
            + "n.downvotesCount = n.downvotesCount + :dDown WHERE n.id = :noteId")
    int adjustCounts(@Param("noteId") String noteId,
                     @Param("dUp") int dUp,
                     @Param("dDown") int dDown);
}
