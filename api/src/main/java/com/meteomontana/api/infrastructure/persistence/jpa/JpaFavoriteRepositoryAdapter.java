package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.port.FavoriteRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class JpaFavoriteRepositoryAdapter implements FavoriteRepository {

    private final SpringDataFavoriteRepository jpaRepo;

    public JpaFavoriteRepositoryAdapter(SpringDataFavoriteRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public void add(String uid, String schoolId) {
        jpaRepo.save(new FavoriteJpaEntity(uid, schoolId, LocalDateTime.now()));
    }

    @Override
    public void remove(String uid, String schoolId) {
        jpaRepo.deleteById(new FavoriteJpaEntity.FavoriteId(uid, schoolId));
    }

    @Override
    public boolean exists(String uid, String schoolId) {
        return jpaRepo.existsById(new FavoriteJpaEntity.FavoriteId(uid, schoolId));
    }

    @Override
    public List<String> findSchoolIdsByUid(String uid) {
        return jpaRepo.findSchoolIdsByUid(uid);
    }
}
