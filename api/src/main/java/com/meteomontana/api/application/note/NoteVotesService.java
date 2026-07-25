package com.meteomontana.api.application.note;

import com.meteomontana.api.domain.exception.BadRequestException;
import com.meteomontana.api.domain.exception.NotFoundException;
import com.meteomontana.api.domain.model.Note;
import com.meteomontana.api.infrastructure.persistence.jpa.NoteVoteJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataNoteVoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Votos de utilidad de las notas comunitarias ("me gusta / no me gusta").
 * Un voto por usuario y nota; repetir el mismo voto lo quita (toggle).
 * Las notas se sirven ordenadas por utilidad (up − down) y luego por fecha.
 */
@Service
public class NoteVotesService {

    /** Nota + el voto del usuario que consulta (0 si no votó o es anónimo). */
    public record NoteWithMyVote(String id, String schoolId, String text, String author,
                                 String uid, java.time.LocalDateTime createdAt,
                                 int upvotesCount, int downvotesCount, String photoUrl,
                                 int myVote) {}

    private final SpringDataNoteVoteRepository votes;

    public NoteVotesService(SpringDataNoteVoteRepository votes) {
        this.votes = votes;
    }

    /**
     * Aplica el voto (1 = me gusta, -1 = no me gusta). Si el usuario repite
     * su voto actual, se retira (value efectivo 0). Devuelve el voto vigente.
     */
    @Transactional
    public int vote(String uid, String noteId, int value) {
        if (value != 1 && value != -1) {
            throw new BadRequestException("value debe ser 1 o -1");
        }
        NoteVoteJpaEntity existing = votes.findByNoteIdAndUid(noteId, uid).orElse(null);
        int old = existing == null ? 0 : existing.getVoteValue();
        int neu = (old == value) ? 0 : value;   // repetir = quitar

        if (neu == 0 && existing != null) {
            votes.delete(existing);
        } else if (existing != null) {
            existing.setVoteValue(neu);
            votes.save(existing);
        } else if (neu != 0) {
            votes.save(new NoteVoteJpaEntity(noteId, uid, neu));
        }
        int dUp = (neu == 1 ? 1 : 0) - (old == 1 ? 1 : 0);
        int dDown = (neu == -1 ? 1 : 0) - (old == -1 ? 1 : 0);
        if (dUp != 0 || dDown != 0) {
            if (votes.adjustCounts(noteId, dUp, dDown) == 0) {
                throw new NotFoundException("nota no encontrada");
            }
        }
        return neu;
    }

    /** Enriquete con myVote y ordena por utilidad (después por fecha, recientes antes). */
    @Transactional(readOnly = true)
    public List<NoteWithMyVote> enrichAndSort(List<Note> notes, String uid) {
        Map<String, Integer> mine = (uid == null || notes.isEmpty()) ? Map.of()
                : votes.findByUidAndNoteIdIn(uid, notes.stream().map(Note::getId).toList())
                    .stream()
                    .collect(Collectors.toMap(NoteVoteJpaEntity::getNoteId,
                                              NoteVoteJpaEntity::getVoteValue));
        return notes.stream()
                .map(n -> new NoteWithMyVote(n.getId(), n.getSchoolId(), n.getText(),
                        n.getAuthor(), n.getUid(), n.getCreatedAt(),
                        n.getUpvotesCount(), n.getDownvotesCount(), n.getPhotoUrl(),
                        mine.getOrDefault(n.getId(), 0)))
                .sorted(Comparator
                        .comparingInt((NoteWithMyVote n) -> n.upvotesCount() - n.downvotesCount())
                        .reversed()
                        .thenComparing(NoteWithMyVote::createdAt,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }
}
