package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.infrastructure.persistence.jpa.CommunityVoteJpaEntities.GradeVoteJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Ver nota de SpringDataOrientationVoteRepository (top-level a proposito). */
public interface SpringDataGradeVoteRepository
        extends JpaRepository<GradeVoteJpaEntity, String> {
    List<GradeVoteJpaEntity> findByLineId(String lineId);
}
