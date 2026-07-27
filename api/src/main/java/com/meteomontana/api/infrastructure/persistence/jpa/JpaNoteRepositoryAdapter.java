package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.exception.SchoolNotFoundException;
import com.meteomontana.api.domain.model.Note;
import com.meteomontana.api.domain.port.NoteRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaNoteRepositoryAdapter implements NoteRepository {

    @Override
    public long count() { return jpaRepo.count(); }

    private final SpringDataNoteRepository jpaRepo;
    private final SpringDataSchoolRepository schoolJpaRepo;

    public JpaNoteRepositoryAdapter(SpringDataNoteRepository jpaRepo,
                                    SpringDataSchoolRepository schoolJpaRepo) {
        this.jpaRepo = jpaRepo;
        this.schoolJpaRepo = schoolJpaRepo;
    }

    @Override
    public List<Note> findRecent(int limit) {
        return jpaRepo.findAll(org.springframework.data.domain.PageRequest.of(0, limit,
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "createdAt")))
                .stream().map(this::toNote).toList();
    }

    @Override
    public List<Note> findBySchoolId(String schoolId) {
        return jpaRepo.findBySchoolId(schoolId).stream()
                .map(this::toNote)
                .toList();
    }

    @Override
    public Optional<Note> findById(String id) {
        return jpaRepo.findById(id).map(this::toNote);
    }

    @Override
    public Note save(Note n) {
        SchoolJpaEntity school = schoolJpaRepo.findById(n.getSchoolId())
                .orElseThrow(() -> new SchoolNotFoundException(n.getSchoolId()));
        NoteJpaEntity e = new NoteJpaEntity(
                n.getId(), school, n.getText(), n.getAuthor(), n.getUid(),
                n.getCreatedAt(), n.getUpvotesCount(), n.getDownvotesCount(),
                n.getPhotoUrl()
        );
        return toNote(jpaRepo.save(e));
    }

    @Override
    public void deleteById(String id) {
        jpaRepo.deleteById(id);
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
                e.getDownvotesCount(),
                e.getPhotoUrl()
        );
    }
}
