package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpringDataFeedCommentLikeRepository
        extends JpaRepository<FeedCommentLikeJpaEntity, FeedCommentLikeJpaEntity.Key> {

    /** Contador de likes por comentario, en una sola query para todo el hilo. */
    @Query("select l.commentId, count(l) from FeedCommentLikeJpaEntity l where l.commentId in :ids group by l.commentId")
    List<Object[]> countByCommentIds(@Param("ids") List<String> ids);

    /** Comentarios del hilo a los que el caller ya dio like. */
    @Query("select l.commentId from FeedCommentLikeJpaEntity l where l.uid = :uid and l.commentId in :ids")
    List<String> likedCommentIds(@Param("uid") String uid, @Param("ids") List<String> ids);

    long countByCommentId(String commentId);
}
