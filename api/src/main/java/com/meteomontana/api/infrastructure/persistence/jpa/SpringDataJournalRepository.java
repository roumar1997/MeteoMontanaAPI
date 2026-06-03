package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataJournalRepository
        extends JpaRepository<JournalSessionJpaEntity, String> {

    List<JournalSessionJpaEntity> findByUidOrderBySessionDateDesc(String uid);
}
