package com.meteomontana.api.application.feed;

import com.meteomontana.api.application.feed.FeedViews.FeedAuthor;
import com.meteomontana.api.application.feed.FeedViews.FeedCommentView;
import com.meteomontana.api.application.moderation.UserModerationService;
import com.meteomontana.api.domain.exception.BadRequestException;
import com.meteomontana.api.domain.exception.ForbiddenException;
import com.meteomontana.api.domain.exception.NotFoundException;
import com.meteomontana.api.domain.model.FeedComment;
import com.meteomontana.api.domain.port.FeedCommentLikeRepository;
import com.meteomontana.api.domain.port.FeedCommentRepository;
import com.meteomontana.api.domain.port.FeedPostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/**
 * COMENTARIOS del feed: listar (con filtro de bloqueados y likes), crear
 * (raíz o respuesta), borrar, y likes de comentario. Las notificaciones
 * (dueño + comentaristas previos + menciones) las dispara {@link FeedNotifier}.
 */
@Service
@RequiredArgsConstructor
public class FeedCommentService {

    private final FeedPostRepository posts;
    private final FeedCommentRepository comments;
    private final FeedCommentLikeRepository commentLikes;
    private final UserModerationService moderation;
    private final FeedAccessGuard guard;
    private final FeedViewMapper viewMapper;
    private final FeedNotifier notifier;

    @Transactional(readOnly = true)
    public List<FeedCommentView> listComments(String uid, long postId) {
        Set<String> blocked = guard.blockedUids(uid);
        List<FeedComment> page = comments.findByPostId(postId);
        Map<String, FeedAuthor> authors = viewMapper.loadAuthors(
                page.stream().map(FeedComment::uid).distinct().toList());
        List<String> ids = page.stream().map(FeedComment::id).toList();
        Map<String, Long> likeCounts = commentLikes.countByCommentIds(ids);
        Set<String> likedByMe = commentLikes.likedCommentIds(uid, ids);
        return page.stream()
                .filter(c -> !blocked.contains(c.uid()))
                .map(c -> new FeedCommentView(c.id(), c.postId(), c.uid(),
                        FeedViewMapper.commentAuthor(authors, c), c.text(), c.createdAt(),
                        c.uid().equals(uid),
                        likeCounts.getOrDefault(c.id(), 0L),
                        likedByMe.contains(c.id()), c.parentId()))
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
            FeedComment parent = comments.findById(parentId)
                    .orElseThrow(() -> new NotFoundException("comentario no encontrado"));
            if (parent.postId() != postId) {
                throw new BadRequestException("el comentario no es de este post");
            }
        }
        String trimmed = text.trim();
        if (trimmed.length() > 1000) trimmed = trimmed.substring(0, 1000);

        String author = notifier.authorLabelOf(uid);

        // Comentaristas previos ANTES de guardar el nuevo (para no notificarse a sí mismo).
        List<FeedComment> previous = comments.findByPostId(postId);

        FeedComment saved = comments.create(new FeedComment(
                UUID.randomUUID().toString(), postId, uid, author, trimmed, parentId, null));

        posts.findById(postId).ifPresent(post -> notifier.notifyComment(uid, author, post, previous));
        notifier.notifyMentions(uid, author, trimmed, postId);

        return new FeedCommentView(saved.id(), saved.postId(), saved.uid(),
                FeedViewMapper.commentAuthor(viewMapper.loadAuthors(List.of(uid)), saved),
                saved.text(), saved.createdAt(), true,
                0L, false, saved.parentId());
    }

    /** Da like a un comentario (idempotente). Devuelve el contador resultante. */
    @Transactional
    public long likeComment(String uid, String commentId) {
        FeedComment c = comments.findById(commentId)
                .orElseThrow(() -> new NotFoundException("comentario no encontrado"));
        if (!commentLikes.exists(commentId, uid)) {
            commentLikes.add(commentId, uid);
            // Solo al CREAR el like y nunca a uno mismo (mismo criterio que el post).
            if (!c.uid().equals(uid)) notifier.notifyCommentLike(uid, c);
        }
        return commentLikes.countByCommentId(commentId);
    }

    /** Quita el like a un comentario (idempotente). Devuelve el contador. */
    @Transactional
    public long unlikeComment(String uid, String commentId) {
        commentLikes.remove(commentId, uid);
        return commentLikes.countByCommentId(commentId);
    }

    /** Borra un comentario propio (o cualquiera si es admin). */
    @Transactional
    public void deleteComment(String uid, String commentId, boolean isAdmin) {
        FeedComment c = comments.findById(commentId)
                .orElseThrow(() -> new NotFoundException("comentario no encontrado"));
        if (!isAdmin && !c.uid().equals(uid)) {
            throw new ForbiddenException("solo puedes borrar tus comentarios");
        }
        comments.deleteById(c.id());
    }
}
