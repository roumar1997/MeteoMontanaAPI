package com.meteomontana.api.application.comments;

import com.meteomontana.api.domain.port.UserRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.LineCommentJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.LineCommentVoteJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataLineCommentRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataLineCommentVoteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

    private final SpringDataLineCommentRepository comments;
    private final SpringDataLineCommentVoteRepository votes;
    private final UserRepository users;
    private final com.meteomontana.api.infrastructure.persistence.jpa.SpringDataUserBlockRepository blocks;
    private final com.meteomontana.api.application.moderation.UserModerationService moderation;

    public LineCommentService(SpringDataLineCommentRepository comments,
                              SpringDataLineCommentVoteRepository votes,
                              UserRepository users,
                              com.meteomontana.api.infrastructure.persistence.jpa.SpringDataUserBlockRepository blocks,
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
        java.util.Set<String> blocked = uid == null ? java.util.Set.of()
                : blocks.findByBlockerUid(uid).stream()
                    .map(com.meteomontana.api.infrastructure.persistence.jpa.UserBlockJpaEntity::getBlockedUid)
                    .collect(Collectors.toSet());
        List<LineCommentJpaEntity> all = comments.findByBlockId(blockId).stream()
                .filter(c -> lineId == null || lineId.equals(c.getLineId()))
                .filter(c -> !blocked.contains(c.getUid()))
                .toList();
        Map<String, Integer> mine = (uid == null || all.isEmpty()) ? Map.of()
                : votes.findByUidAndCommentIdIn(uid, all.stream().map(LineCommentJpaEntity::getId).toList())
                    .stream()
                    .collect(Collectors.toMap(LineCommentVoteJpaEntity::getCommentId,
                                              LineCommentVoteJpaEntity::getVoteValue));
        return all.stream()
                .map(c -> new CommentView(c.getId(), c.getBlockId(), c.getLineId(), c.getAuthor(),
                        c.getUid(), c.getCreatedAt(), c.getText(),
                        c.getUpvotesCount(), c.getDownvotesCount(),
                        mine.getOrDefault(c.getId(), 0)))
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "text is required");
        }
        String trimmed = text.trim();
        if (trimmed.length() > 1000) trimmed = trimmed.substring(0, 1000);

        String author = users.findByUid(uid)
                .map(u -> u.getUsername() != null ? "@" + u.getUsername()
                        : (u.getDisplayName() != null ? u.getDisplayName() : "Anónimo"))
                .orElse("Anónimo");

        LineCommentJpaEntity saved = comments.save(new LineCommentJpaEntity(
                UUID.randomUUID().toString(), blockId,
                (lineId == null || lineId.isBlank()) ? null : lineId,
                uid, author, trimmed));
        return new CommentView(saved.getId(), saved.getBlockId(), saved.getLineId(),
                saved.getAuthor(), saved.getUid(), saved.getCreatedAt(), saved.getText(),
                0, 0, 0);
    }

    /** Borra un comentario propio (o cualquiera si es admin). */
    @Transactional
    public void delete(String uid, String commentId, boolean isAdmin) {
        LineCommentJpaEntity c = comments.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "comentario no encontrado"));
        if (!isAdmin && !c.getUid().equals(uid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "solo puedes borrar tus comentarios");
        }
        comments.delete(c);
    }

    /** Voto ±1; repetir el voto vigente lo retira. Devuelve el voto resultante. */
    @Transactional
    public int vote(String uid, String commentId, int value) {
        if (value != 1 && value != -1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "value debe ser 1 o -1");
        }
        LineCommentVoteJpaEntity existing = votes.findByCommentIdAndUid(commentId, uid).orElse(null);
        int old = existing == null ? 0 : existing.getVoteValue();
        int neu = (old == value) ? 0 : value;

        if (neu == 0 && existing != null) {
            votes.delete(existing);
        } else if (existing != null) {
            existing.setVoteValue(neu);
            votes.save(existing);
        } else if (neu != 0) {
            votes.save(new LineCommentVoteJpaEntity(commentId, uid, neu));
        }
        int dUp = (neu == 1 ? 1 : 0) - (old == 1 ? 1 : 0);
        int dDown = (neu == -1 ? 1 : 0) - (old == -1 ? 1 : 0);
        if (dUp != 0 || dDown != 0) {
            if (votes.adjustCounts(commentId, dUp, dDown) == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "comentario no encontrado");
            }
        }
        return neu;
    }
}
