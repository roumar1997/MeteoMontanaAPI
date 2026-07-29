package com.meteomontana.api.domain.model;

import java.time.LocalDateTime;

/**
 * Comentario de la comunidad en una piedra/muro (lineId=null) o en una vía
 * concreta. Con contadores de utilidad agregados, como las notas de escuela.
 * {@code createdAt} es null hasta que la persistencia crea la fila.
 */
public record LineComment(
        String id,
        String blockId,
        String lineId,
        String uid,
        String author,
        String text,
        int upvotesCount,
        int downvotesCount,
        LocalDateTime createdAt
) {}
