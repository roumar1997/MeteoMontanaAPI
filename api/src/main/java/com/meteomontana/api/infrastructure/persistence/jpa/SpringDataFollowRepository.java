package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataFollowRepository
        extends JpaRepository<FollowJpaEntity, FollowJpaEntity.FollowId> {

    /** Borrado de cuenta: todas las relaciones donde el usuario es follower o followed. */
    @Modifying
    @Query("DELETE FROM FollowJpaEntity f WHERE f.id.followerUid = :uid OR f.id.followedUid = :uid")
    void deleteAllForUid(@Param("uid") String uid);

    @Query("SELECT f.id.followerUid FROM FollowJpaEntity f " +
           "WHERE f.id.followedUid = :uid AND f.status = 'ACCEPTED'")
    List<String> findFollowersOf(@Param("uid") String uid);

    @Query("SELECT f.id.followedUid FROM FollowJpaEntity f " +
           "WHERE f.id.followerUid = :uid AND f.status = 'ACCEPTED'")
    List<String> findFollowingOf(@Param("uid") String uid);

    @Query("SELECT COUNT(f) FROM FollowJpaEntity f " +
           "WHERE f.id.followedUid = :uid AND f.status = 'ACCEPTED'")
    long countAcceptedFollowers(@Param("uid") String uid);

    @Query("SELECT COUNT(f) FROM FollowJpaEntity f " +
           "WHERE f.id.followerUid = :uid AND f.status = 'ACCEPTED'")
    long countAcceptedFollowing(@Param("uid") String uid);

    @Query("SELECT f.id.followerUid FROM FollowJpaEntity f " +
           "WHERE f.id.followedUid = :uid AND f.status = 'PENDING' " +
           "ORDER BY f.createdAt DESC")
    List<String> findPendingRequestsFor(@Param("uid") String uid);

    Optional<FollowJpaEntity> findById_FollowerUidAndId_FollowedUid(String followerUid, String followedUid);
}
