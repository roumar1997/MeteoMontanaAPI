package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpringDataFavoriteRepository
        extends JpaRepository<FavoriteJpaEntity, FavoriteJpaEntity.FavoriteId> {

    @Query("SELECT f.id.schoolId FROM FavoriteJpaEntity f WHERE f.id.uid = :uid ORDER BY f.createdAt DESC")
    List<String> findSchoolIdsByUid(@Param("uid") String uid);

    /** Borrado de cuenta: todas las favoritas del usuario. */
    @Modifying
    @Query("DELETE FROM FavoriteJpaEntity f WHERE f.id.uid = :uid")
    void deleteByUid(@Param("uid") String uid);
}
