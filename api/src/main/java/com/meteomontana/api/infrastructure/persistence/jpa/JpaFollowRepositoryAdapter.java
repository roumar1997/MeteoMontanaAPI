package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.port.FollowRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class JpaFollowRepositoryAdapter implements FollowRepository {

    private final SpringDataFollowRepository jpaRepo;

    public JpaFollowRepositoryAdapter(SpringDataFollowRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public void add(String follower, String followed) {
        jpaRepo.save(new FollowJpaEntity(follower, followed, LocalDateTime.now()));
    }

    @Override
    public void remove(String follower, String followed) {
        jpaRepo.deleteById(new FollowJpaEntity.FollowId(follower, followed));
    }

    @Override
    public boolean isFollowing(String follower, String followed) {
        return jpaRepo.existsById(new FollowJpaEntity.FollowId(follower, followed));
    }

    @Override public List<String> followersOf(String uid) { return jpaRepo.findFollowersOf(uid); }
    @Override public List<String> followingOf(String uid) { return jpaRepo.findFollowingOf(uid); }
    @Override public long countFollowers(String uid)      { return jpaRepo.countByIdFollowedUid(uid); }
    @Override public long countFollowing(String uid)      { return jpaRepo.countByIdFollowerUid(uid); }
}
