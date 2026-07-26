package com.meteomontana.api.domain.port;

import com.meteomontana.api.domain.model.LineComment;

import java.util.List;
import java.util.Optional;

/** Comentarios de piedras/vías con contadores agregados (puerto de dominio). */
public interface LineCommentRepository {

    List<LineComment> findByBlockId(String blockId);

    Optional<LineComment> findById(String id);

    /** Crea la fila y devuelve el comentario con createdAt asignado. */
    LineComment create(LineComment comment);

    void deleteById(String id);

    /** Ajuste ATÓMICO de los contadores agregados. @return filas afectadas
     *  (0 = el comentario ya no existe). */
    int adjustVoteCounts(String commentId, int deltaUp, int deltaDown);
}
