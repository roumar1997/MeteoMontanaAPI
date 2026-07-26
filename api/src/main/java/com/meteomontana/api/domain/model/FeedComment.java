package com.meteomontana.api.domain.model;

import java.time.LocalDateTime;

/**
 * Comentario en un post del feed. Separado de los comentarios de vía a
 * propósito: "¡qué máquina!" es sobre el ascenso de una persona, no
 * información de la vía. {@code author} es el snapshot del nombre al
 * comentar (respaldo si la cuenta se borra); {@code parentId} enlaza la
 * respuesta con su comentario raíz (null = comentario raíz).
 *
 * {@code createdAt} es null hasta que la persistencia crea la fila.
 */
public record FeedComment(
        String id,
        long postId,
        String uid,
        String author,
        String text,
        String parentId,
        LocalDateTime createdAt
) {}
