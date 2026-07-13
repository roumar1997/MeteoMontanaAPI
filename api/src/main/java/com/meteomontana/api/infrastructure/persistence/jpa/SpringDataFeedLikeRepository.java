package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpringDataFeedLikeRepository extends JpaRepository<FeedLikeJpaEntity, FeedLikeJpaEntity.Key> {

    /** Contador de likes por post, en una sola query para toda la página. */
    @Query("select l.postId, count(l) from FeedLikeJpaEntity l where l.postId in :ids group by l.postId")
    List<Object[]> countByPostIds(@Param("ids") List<Long> ids);

    /** Posts de la página a los que el caller ya dio like. */
    @Query("select l.postId from FeedLikeJpaEntity l where l.uid = :uid and l.postId in :ids")
    List<Long> likedPostIds(@Param("uid") String uid, @Param("ids") List<Long> ids);
}
