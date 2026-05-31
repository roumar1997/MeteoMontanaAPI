package com.meteomontana.api.application;

import com.meteomontana.api.domain.model.Note;
import com.meteomontana.api.domain.port.NoteRepository;
import com.meteomontana.api.domain.port.SchoolRepository;
import com.meteomontana.api.domain.exception.SchoolNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetNotesBySchoolUseCase {

    private final NoteRepository noteRepository;
    private final SchoolRepository schoolRepository;

    public GetNotesBySchoolUseCase(NoteRepository noteRepository,
                                   SchoolRepository schoolRepository) {
        this.noteRepository = noteRepository;
        this.schoolRepository = schoolRepository;
    }

    public List<Note> execute(String schoolId) {
        schoolRepository.findById(schoolId)
                .orElseThrow(() -> new SchoolNotFoundException(schoolId));
        return noteRepository.findBySchoolId(schoolId);
    }
}
