package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataLineCommentRepository extends JpaRepository<LineCommentJpaEntity, String> {
    List<LineCommentJpaEntity> findByBlockId(String blockId);
}
