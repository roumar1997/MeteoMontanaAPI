package com.meteomontana.api.application.comments;

import com.meteomontana.api.domain.exception.BadRequestException;
import com.meteomontana.api.domain.exception.ForbiddenException;
import com.meteomontana.api.domain.exception.NotFoundException;
import com.meteomontana.api.domain.model.LineComment;
import com.meteomontana.api.domain.port.LineCommentRepository;
import com.meteomontana.api.domain.port.LineCommentVoteRepository;
import com.meteomontana.api.domain.port.UserBlockRepository;
import com.meteomontana.api.domain.port.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Comentarios de la comunidad en piedras/muros y vías, con votos de utilidad.
 * Mismo modelo que las notas de escuela (NoteVotesService): un voto por
 * usuario, repetir voto lo retira, orden por (up-down) desc y luego recientes.
 */
@Service
public class LineCommentService {

    public record CommentView(String id, String blockId, String lineId, String author,
                              String uid, LocalDateTime createdAt, String text,
                              int upvotesCount, int downvotesCount, int myVote) {}

    private final LineCommentRepository comments;
    private final LineCommentVoteRepository votes;
    private final UserRepository users;
    private final UserBlockRepository blocks;
    private final com.meteomontana.api.application.moderation.UserModerationService moderation;

    public LineCommentService(LineCommentRepository comments,
                              LineCommentVoteRepository votes,
                              UserRepository users,
                              UserBlockRepository blocks,
                              com.meteomontana.api.application.moderation.UserModerationService moderation) {
        this.comments = comments;
        this.votes = votes;
        this.users = users;
        this.blocks = blocks;
        this.moderation = moderation;
    }

    /** Comentarios del bloque (y opcionalmente de UNA vía), ordenados por utilidad. */
    @Transactional(readOnly = true)
    public List<CommentView> list(String blockId, String lineId, String uid) {
        // El bloqueador no ve el contenido de sus bloqueados.
        Set<String> blocked = uid == null ? Set.of() : blocks.blockedUidsOf(uid);
        List<LineComment> all = comments.findByBlockId(blockId).stream()
                .filter(c -> lineId == null || lineId.equals(c.lineId()))
                .filter(c -> !blocked.contains(c.uid()))
                .toList();
        Map<String, Integer> mine = (uid == null || all.isEmpty()) ? Map.of()
                : votes.votesOf(uid, all.stream().map(LineComment::id).toList());
        return all.stream()
                .map(c -> new CommentView(c.id(), c.blockId(), c.lineId(), c.author(),
                        c.uid(), c.createdAt(), c.text(),
                        c.upvotesCount(), c.downvotesCount(),
                        mine.getOrDefault(c.id(), 0)))
                .sorted(Comparator
                        .comparingInt((CommentView c) -> c.upvotesCount() - c.downvotesCount())
                        .reversed()
                        .thenComparing(CommentView::createdAt,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional
    public CommentView add(String uid, String blockId, String lineId, String text) {
        moderation.ensureCanPost(uid);   // baneado/suspendido → 403
        if (text == null || text.isBlank()) {
            throw new BadRequestException("text is required");
        }
        String trimmed = text.trim();
        if (trimmed.length() > 1000) trimmed = trimmed.substring(0, 1000);

        String author = users.findByUid(uid)
                .map(u -> u.getUsername() != null ? "@" + u.getUsername()
                        : (u.getDisplayName() != null ? u.getDisplayName() : "Anónimo"))
                .orElse("Anónimo");

        LineComment saved = comments.create(new LineComment(
                UUID.randomUUID().toString(), blockId,
                (lineId == null || lineId.isBlank()) ? null : lineId,
                uid, author, trimmed, 0, 0, null));
        return new CommentView(saved.id(), saved.blockId(), saved.lineId(),
                saved.author(), saved.uid(), saved.createdAt(), saved.text(),
                0, 0, 0);
    }

    /** Borra un comentario propio (o cualquiera si es admin). */
    @Transactional
    public void delete(String uid, String commentId, boolean isAdmin) {
        LineComment c = comments.findById(commentId)
                .orElseThrow(() -> new NotFoundException("comentario no encontrado"));
        if (!isAdmin && !c.uid().equals(uid)) {
            throw new ForbiddenException("solo puedes borrar tus comentarios");
        }
        comments.deleteById(c.id());
    }

    /** Voto ±1; repetir el voto vigente lo retira. Devuelve el voto resultante. */
    @Transactional
    public int vote(String uid, String commentId, int value) {
        if (value != 1 && value != -1) {
            throw new BadRequestException("value debe ser 1 o -1");
        }
        int old = votes.voteOf(commentId, uid);
        int neu = (old == value) ? 0 : value;

        if (neu == 0 && old != 0) {
            votes.removeVote(commentId, uid);
        } else if (neu != 0) {
            votes.setVote(commentId, uid, neu);
        }
        int dUp = (neu == 1 ? 1 : 0) - (old == 1 ? 1 : 0);
        int dDown = (neu == -1 ? 1 : 0) - (old == -1 ? 1 : 0);
        if (dUp != 0 || dDown != 0) {
            if (comments.adjustVoteCounts(commentId, dUp, dDown) == 0) {
                throw new NotFoundException("comentario no encontrado");
            }
        }
        return neu;
    }
}
