package com.meteomontana.api.domain.port;

import java.util.List;
import java.util.Map;

/** Votos ±1 a comentarios de piedra/vía: un voto por usuario (puerto de dominio). */
public interface LineCommentVoteRepository {

    /** Mi voto vigente sobre ese comentario (0 si no he votado). */
    int voteOf(String commentId, String uid);

    /** Mis votos sobre un conjunto de comentarios: commentId → ±1. */
    Map<String, Integer> votesOf(String uid, List<String> commentIds);

    /** Fija mi voto (crea o actualiza la fila). */
    void setVote(String commentId, String uid, int value);

    /** Retira mi voto (si existía). */
    void removeVote(String commentId, String uid);
}
