package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpringDataFeedCommentRepository extends JpaRepository<FeedCommentJpaEntity, String> {

    List<FeedCommentJpaEntity> findByPostIdOrderByCreatedAtAsc(Long postId);

    /** Contador de comentarios por post, en una sola query para toda la página. */
    @Query("select c.postId, count(c) from FeedCommentJpaEntity c where c.postId in :ids group by c.postId")
    List<Object[]> countByPostIds(@Param("ids") List<Long> ids);
}
