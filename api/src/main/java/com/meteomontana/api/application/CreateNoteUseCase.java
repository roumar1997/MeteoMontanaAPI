package com.meteomontana.api.application;

import com.meteomontana.api.domain.exception.SchoolNotFoundException;
import com.meteomontana.api.domain.model.Note;
import com.meteomontana.api.domain.port.NoteRepository;
import com.meteomontana.api.domain.port.SchoolRepository;
import com.meteomontana.api.domain.port.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class CreateNoteUseCase {

    public record CreateNoteRequest(String text) {}

    private final NoteRepository noteRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;

    public CreateNoteUseCase(NoteRepository noteRepository,
                             SchoolRepository schoolRepository,
                             UserRepository userRepository) {
        this.noteRepository = noteRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
    }

    public Note execute(String uid, String schoolId, String text) {
        schoolRepository.findById(schoolId)
                .orElseThrow(() -> new SchoolNotFoundException(schoolId));

        if (text == null || text.isBlank())
            throw new IllegalArgumentException("text is required");
        String trimmed = text.trim();
        if (trimmed.length() > 500)
            trimmed = trimmed.substring(0, 500);

        String author = userRepository.findByUid(uid)
                .map(u -> u.getDisplayName() != null ? u.getDisplayName() : u.getEmail())
                .orElse("Anónimo");

        Note note = new Note(
                UUID.randomUUID().toString(),
                schoolId,
                trimmed,
                author,
                uid,
                LocalDateTime.now(),
                0, 0
        );
        return noteRepository.save(note);
    }
}
