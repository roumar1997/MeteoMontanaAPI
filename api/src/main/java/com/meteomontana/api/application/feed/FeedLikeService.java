package com.meteomontana.api.application.feed;

import com.meteomontana.api.domain.exception.NotFoundException;
import com.meteomontana.api.infrastructure.persistence.jpa.FeedLikeJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.FeedPostJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataFeedLikeRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataFeedPostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * LIKES de posts del feed: dar/quitar (idempotentes) y notificación al dueño
 * solo al CREAR el like y nunca a uno mismo.
 */
@Service
public class FeedLikeService {

    private final SpringDataFeedPostRepository posts;
    private final SpringDataFeedLikeRepository likes;
    private final FeedNotifier notifier;

    public FeedLikeService(SpringDataFeedPostRepository posts,
                           SpringDataFeedLikeRepository likes,
                           FeedNotifier notifier) {
        this.posts = posts;
        this.likes = likes;
        this.notifier = notifier;
    }

    /** Da like (idempotente). Devuelve el contador resultante. */
    @Transactional
    public long like(String uid, long postId) {
        FeedPostJpaEntity post = posts.findById(postId)
                .orElseThrow(() -> new NotFoundException("post no encontrado"));
        if (!likes.existsById(new FeedLikeJpaEntity.Key(postId, uid))) {
            likes.save(new FeedLikeJpaEntity(postId, uid));
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
        FeedLikeJpaEntity.Key key = new FeedLikeJpaEntity.Key(postId, uid);
        if (likes.existsById(key)) likes.deleteById(key);
        return countLikes(postId);
    }

    private long countLikes(long postId) {
        Map<Long, Long> counts = FeedViewMapper.toCountMap(likes.countByPostIds(List.of(postId)));
        return counts.getOrDefault(postId, 0L);
    }
}
