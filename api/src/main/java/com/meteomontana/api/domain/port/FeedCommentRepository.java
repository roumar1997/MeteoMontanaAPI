package com.meteomontana.api.domain.port;

import com.meteomontana.api.domain.model.FeedComment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Comentarios de posts del feed (puerto de dominio, sin JPA). */
public interface FeedCommentRepository {

    /** Comentarios del post en orden cronológico ascendente. */
    List<FeedComment> findByPostId(long postId);

    Optional<FeedComment> findById(String id);

    /** Crea la fila y devuelve el comentario con createdAt asignado. */
    FeedComment create(FeedComment comment);

    void deleteById(String id);

    /** Contador de comentarios por post, en una sola query para toda la página. */
    Map<Long, Long> countByPostIds(List<Long> postIds);
}
