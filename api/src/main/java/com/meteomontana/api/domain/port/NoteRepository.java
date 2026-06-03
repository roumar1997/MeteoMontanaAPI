package com.meteomontana.api.domain.port;

import com.meteomontana.api.domain.model.Note;

import java.util.List;
import java.util.Optional;

public interface NoteRepository {
    List<Note> findBySchoolId(String schoolId);
    Note save(Note note);
    Optional<Note> findById(String id);
    void deleteById(String id);
}
