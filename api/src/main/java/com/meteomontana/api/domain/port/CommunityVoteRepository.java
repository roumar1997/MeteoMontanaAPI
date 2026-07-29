package com.meteomontana.api.domain.port;

import com.meteomontana.api.domain.model.CommunityVotes.GradeVote;
import com.meteomontana.api.domain.model.CommunityVotes.OrientationVote;

import java.util.List;

/**
 * Votos comunitarios de orientación y grado (puerto de dominio).
 * El upsert es "un voto por persona y superficie": votar de nuevo sustituye.
 */
public interface CommunityVoteRepository {

    List<OrientationVote> findOrientationVotes(String blockId);

    /** Votos de VARIOS bloques en una sola query (filtro por orientación de una escuela). */
    List<OrientationVote> findOrientationVotesForBlocks(java.util.Collection<String> blockIds);

    void upsertOrientationVote(OrientationVote vote);

    List<GradeVote> findGradeVotes(String lineId);

    void upsertGradeVote(GradeVote vote);

    /**
     * Propaga el grado MOSTRADO de una vía: actualiza block_lines.grade y los
     * diarios de todos los usuarios con esa vía (decisión de Rodrigo: el
     * consenso cambia el grado también en los perfiles).
     */
    void applyDisplayedGrade(String lineId, String displayedGrade);
}
