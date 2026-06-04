package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpringDataFollowRepository
        extends JpaRepository<FollowJpaEntity, FollowJpaEntity.FollowId> {

    @Query("SELECT f.id.followerUid FROM FollowJpaEntity f WHERE f.id.followedUid = :uid")
    List<String> findFollowersOf(@Param("uid") String uid);

    @Query("SELECT f.id.followedUid FROM FollowJpaEntity f WHERE f.id.followerUid = :uid")
    List<String> findFollowingOf(@Param("uid") String uid);

    long countByIdFollowedUid(String uid);
    long countByIdFollowerUid(String uid);
}
