package com.meteomontana.api.domain.port;

import com.meteomontana.api.domain.model.Note;
import java.util.List;

public interface NoteRepository {
    List<Note> findBySchoolId(String schoolId);
}
