package com.meteomontana.api.domain.port;

import java.util.List;
import java.util.Map;

/** Votos de utilidad de las notas comunitarias (puerto de dominio). */
public interface NoteVoteRepository {

    /** Mi voto vigente sobre esa nota (0 si no he votado). */
    int voteOf(String noteId, String uid);

    /** Mis votos sobre un conjunto de notas: noteId → ±1. */
    Map<String, Integer> votesOf(String uid, List<String> noteIds);

    /** Fija mi voto (crea o actualiza la fila). */
    void setVote(String noteId, String uid, int value);

    /** Retira mi voto (si existía). */
    void removeVote(String noteId, String uid);

    /** Ajuste ATÓMICO de los contadores agregados de la nota.
     *  @return filas afectadas (0 = la nota ya no existe). */
    int adjustVoteCounts(String noteId, int deltaUp, int deltaDown);
}
