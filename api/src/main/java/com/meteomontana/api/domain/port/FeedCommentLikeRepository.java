package com.meteomontana.api.domain.port;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Likes de COMENTARIOS del feed: un like por usuario (puerto de dominio). */
public interface FeedCommentLikeRepository {

    boolean exists(String commentId, String uid);

    void add(String commentId, String uid);

    void remove(String commentId, String uid);

    long countByCommentId(String commentId);

    /** Contador de likes por comentario, en una sola query para todo el hilo. */
    Map<String, Long> countByCommentIds(List<String> commentIds);

    /** Comentarios del hilo a los que el caller ya dio like. */
    Set<String> likedCommentIds(String uid, List<String> commentIds);
}
