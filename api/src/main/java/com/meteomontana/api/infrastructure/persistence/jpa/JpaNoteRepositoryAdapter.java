package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.Note;
import com.meteomontana.api.domain.port.NoteRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JpaNoteRepositoryAdapter implements NoteRepository {

    private final SpringDataNoteRepository jpaRepo;

    public JpaNoteRepositoryAdapter(SpringDataNoteRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public List<Note> findBySchoolId(String schoolId) {
        return jpaRepo.findBySchoolId(schoolId).stream()
                .map(this::toNote)
                .toList();
    }

    private Note toNote(NoteJpaEntity e) {
        return new Note(
                e.getId(),
                e.getSchool().getId(),
                e.getText(),
                e.getAuthor(),
                e.getUid(),
                e.getCreatedAt(),
                e.getUpvotesCount(),
                e.getDownvotesCount()
        );
    }
}
