package com.meteomontana.api.application.feed;

import com.meteomontana.api.application.feed.FeedViews.FeedAuthor;
import com.meteomontana.api.application.feed.FeedViews.FeedCommentView;
import com.meteomontana.api.application.moderation.UserModerationService;
import com.meteomontana.api.domain.exception.BadRequestException;
import com.meteomontana.api.domain.exception.ForbiddenException;
import com.meteomontana.api.domain.exception.NotFoundException;
import com.meteomontana.api.infrastructure.persistence.jpa.FeedCommentJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.FeedCommentLikeJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataFeedCommentLikeRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataFeedCommentRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataFeedPostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * COMENTARIOS del feed: listar (con filtro de bloqueados y likes), crear
 * (raíz o respuesta), borrar, y likes de comentario. Las notificaciones
 * (dueño + comentaristas previos + menciones) las dispara {@link FeedNotifier}.
 */
@Service
public class FeedCommentService {

    private final SpringDataFeedPostRepository posts;
    private final SpringDataFeedCommentRepository comments;
    private final SpringDataFeedCommentLikeRepository commentLikes;
    private final UserModerationService moderation;
    private final FeedAccessGuard guard;
    private final FeedViewMapper viewMapper;
    private final FeedNotifier notifier;

    public FeedCommentService(SpringDataFeedPostRepository posts,
                              SpringDataFeedCommentRepository comments,
                              SpringDataFeedCommentLikeRepository commentLikes,
                              UserModerationService moderation,
                              FeedAccessGuard guard,
                              FeedViewMapper viewMapper,
                              FeedNotifier notifier) {
        this.posts = posts;
        this.comments = comments;
        this.commentLikes = commentLikes;
        this.moderation = moderation;
        this.guard = guard;
        this.viewMapper = viewMapper;
        this.notifier = notifier;
    }

    @Transactional(readOnly = true)
    public List<FeedCommentView> listComments(String uid, long postId) {
        Set<String> blocked = guard.blockedUids(uid);
        List<FeedCommentJpaEntity> page = comments.findByPostIdOrderByCreatedAtAsc(postId);
        Map<String, FeedAuthor> authors = viewMapper.loadAuthors(
                page.stream().map(FeedCommentJpaEntity::getUid).distinct().toList());
        List<String> ids = page.stream().map(FeedCommentJpaEntity::getId).toList();
        Map<String, Long> likeCounts = ids.isEmpty() ? Map.of()
                : commentLikes.countByCommentIds(ids).stream()
                        .collect(Collectors.toMap(r -> (String) r[0], r -> (Long) r[1]));
        Set<String> likedByMe = ids.isEmpty() ? Set.of()
                : Set.copyOf(commentLikes.likedCommentIds(uid, ids));
        return page.stream()
                .filter(c -> !blocked.contains(c.getUid()))
                .map(c -> new FeedCommentView(c.getId(), c.getPostId(), c.getUid(),
                        FeedViewMapper.commentAuthor(authors, c), c.getText(), c.getCreatedAt(),
                        c.getUid().equals(uid),
                        likeCounts.getOrDefault(c.getId(), 0L),
                        likedByMe.contains(c.getId()), c.getParentId()))
                .toList();
    }

    /**
     * @param parentId comentario al que se responde (null = comentario raíz).
     *                 Puede ser una respuesta (se responde a respuestas); las
     *                 apps agrupan visualmente bajo el comentario raíz del hilo
     *                 y mencionan al autor respondido.
     */
    @Transactional
    public FeedCommentView addComment(String uid, long postId, String text, String parentId) {
        moderation.ensureCanPost(uid);
        if (text == null || text.isBlank()) {
            throw new BadRequestException("text is required");
        }
        if (!posts.existsById(postId)) {
            throw new NotFoundException("post no encontrado");
        }
        if (parentId != null) {
            FeedCommentJpaEntity parent = comments.findById(parentId)
                    .orElseThrow(() -> new NotFoundException("comentario no encontrado"));
            if (parent.getPostId() != postId) {
                throw new BadRequestException("el comentario no es de este post");
            }
        }
        String trimmed = text.trim();
        if (trimmed.length() > 1000) trimmed = trimmed.substring(0, 1000);

        String author = notifier.authorLabelOf(uid);

        // Comentaristas previos ANTES de guardar el nuevo (para no notificarse a sí mismo).
        List<FeedCommentJpaEntity> previous = comments.findByPostIdOrderByCreatedAtAsc(postId);

        FeedCommentJpaEntity saved = comments.save(new FeedCommentJpaEntity(
                UUID.randomUUID().toString(), postId, uid, author, trimmed, parentId));

        posts.findById(postId).ifPresent(post -> notifier.notifyComment(uid, author, post, previous));
        notifier.notifyMentions(uid, author, trimmed, postId);

        return new FeedCommentView(saved.getId(), saved.getPostId(), saved.getUid(),
                FeedViewMapper.commentAuthor(viewMapper.loadAuthors(List.of(uid)), saved),
                saved.getText(), saved.getCreatedAt(), true,
                0L, false, saved.getParentId());
    }

    /** Da like a un comentario (idempotente). Devuelve el contador resultante. */
    @Transactional
    public long likeComment(String uid, String commentId) {
        FeedCommentJpaEntity c = comments.findById(commentId)
                .orElseThrow(() -> new NotFoundException("comentario no encontrado"));
        if (!commentLikes.existsById(new FeedCommentLikeJpaEntity.Key(commentId, uid))) {
            commentLikes.save(new FeedCommentLikeJpaEntity(commentId, uid));
            // Solo al CREAR el like y nunca a uno mismo (mismo criterio que el post).
            if (!c.getUid().equals(uid)) notifier.notifyCommentLike(uid, c);
        }
        return commentLikes.countByCommentId(commentId);
    }

    /** Quita el like a un comentario (idempotente). Devuelve el contador. */
    @Transactional
    public long unlikeComment(String uid, String commentId) {
        FeedCommentLikeJpaEntity.Key key = new FeedCommentLikeJpaEntity.Key(commentId, uid);
        if (commentLikes.existsById(key)) commentLikes.deleteById(key);
        return commentLikes.countByCommentId(commentId);
    }

    /** Borra un comentario propio (o cualquiera si es admin). */
    @Transactional
    public void deleteComment(String uid, String commentId, boolean isAdmin) {
        FeedCommentJpaEntity c = comments.findById(commentId)
                .orElseThrow(() -> new NotFoundException("comentario no encontrado"));
        if (!isAdmin && !c.getUid().equals(uid)) {
            throw new ForbiddenException("solo puedes borrar tus comentarios");
        }
        comments.delete(c);
    }
}
