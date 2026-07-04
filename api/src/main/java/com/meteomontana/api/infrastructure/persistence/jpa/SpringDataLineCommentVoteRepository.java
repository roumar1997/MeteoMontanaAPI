package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataLineCommentVoteRepository extends JpaRepository<LineCommentVoteJpaEntity, String> {

    Optional<LineCommentVoteJpaEntity> findByCommentIdAndUid(String commentId, String uid);

    List<LineCommentVoteJpaEntity> findByUidAndCommentIdIn(String uid, List<String> commentIds);

    /** Ajuste atómico de los contadores agregados del comentario. */
    @Modifying
    @Query("UPDATE LineCommentJpaEntity c SET c.upvotesCount = c.upvotesCount + :dUp, "
            + "c.downvotesCount = c.downvotesCount + :dDown WHERE c.id = :commentId")
    int adjustCounts(@Param("commentId") String commentId,
                     @Param("dUp") int dUp,
                     @Param("dDown") int dDown);
}
