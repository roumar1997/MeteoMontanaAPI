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

    /** photoUrl es opcional: URL pública de Firebase Storage si la nota lleva foto. */
    public record CreateNoteRequest(String text, String photoUrl) {}

    private final NoteRepository noteRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final com.meteomontana.api.application.moderation.UserModerationService moderation;

    public CreateNoteUseCase(NoteRepository noteRepository,
                             SchoolRepository schoolRepository,
                             UserRepository userRepository,
                             com.meteomontana.api.application.moderation.UserModerationService moderation) {
        this.noteRepository = noteRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.moderation = moderation;
    }

    public Note execute(String uid, String schoolId, String text, String photoUrl) {
        moderation.ensureCanPost(uid);   // baneado/suspendido → 403
        schoolRepository.findById(schoolId)
                .orElseThrow(() -> new SchoolNotFoundException(schoolId));

        if (text == null || text.isBlank())
            throw new IllegalArgumentException("text is required");
        String trimmed = text.trim();
        if (trimmed.length() > 500)
            trimmed = trimmed.substring(0, 500);

        // La foto se sube desde la app a Firebase Storage; aquí solo guardamos la URL.
        String photo = (photoUrl == null || photoUrl.isBlank()) ? null : photoUrl.trim();
        if (photo != null) {
            if (!photo.startsWith("https://"))
                throw new IllegalArgumentException("photoUrl must be an https URL");
            if (photo.length() > 1000)
                throw new IllegalArgumentException("photoUrl too long");
        }

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
                0, 0,
                photo
        );
        return noteRepository.save(note);
    }
}
