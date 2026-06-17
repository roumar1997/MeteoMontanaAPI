package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SpringDataNoteRepository
        extends JpaRepository<NoteJpaEntity, String> {

    List<NoteJpaEntity> findBySchoolId(String schoolId);

    /** Borrado de cuenta. */
    void deleteByUid(String uid);
}