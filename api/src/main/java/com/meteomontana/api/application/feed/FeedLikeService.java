package com.meteomontana.api.application.feed;

import com.meteomontana.api.domain.exception.NotFoundException;
import com.meteomontana.api.domain.model.FeedPost;
import com.meteomontana.api.domain.port.FeedLikeRepository;
import com.meteomontana.api.domain.port.FeedPostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * LIKES de posts del feed: dar/quitar (idempotentes) y notificación al dueño
 * solo al CREAR el like y nunca a uno mismo.
 */
@Service
public class FeedLikeService {

    private final FeedPostRepository posts;
    private final FeedLikeRepository likes;
    private final FeedNotifier notifier;

    public FeedLikeService(FeedPostRepository posts,
                           FeedLikeRepository likes,
                           FeedNotifier notifier) {
        this.posts = posts;
        this.likes = likes;
        this.notifier = notifier;
    }

    /** Da like (idempotente). Devuelve el contador resultante. */
    @Transactional
    public long like(String uid, long postId) {
        FeedPost post = posts.findById(postId)
                .orElseThrow(() -> new NotFoundException("post no encontrado"));
        if (!likes.exists(postId, uid)) {
            likes.add(postId, uid);
            // Solo al CREAR el like (no en repeticiones ni unlike) y nunca a uno mismo.
            if (!post.getUserUid().equals(uid)) {
                notifier.notifyLike(uid, post);
            }
        }
        return countLikes(postId);
    }

    /** Quita el like (idempotente). Devuelve el contador resultante. */
    @Transactional
    public long unlike(String uid, long postId) {
        likes.remove(postId, uid);
        return countLikes(postId);
    }

    private long countLikes(long postId) {
        return likes.countByPostIds(List.of(postId)).getOrDefault(postId, 0L);
    }
}
