package com.meteomontana.api.domain.port;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Likes de posts del feed: un like por usuario (puerto de dominio). */
public interface FeedLikeRepository {

    boolean exists(long postId, String uid);

    void add(long postId, String uid);

    void remove(long postId, String uid);

    /** Contador de likes por post, en una sola query para toda la página. */
    Map<Long, Long> countByPostIds(List<Long> postIds);

    /** Posts de la página a los que el caller ya dio like. */
    Set<Long> likedPostIds(String uid, List<Long> postIds);
}
