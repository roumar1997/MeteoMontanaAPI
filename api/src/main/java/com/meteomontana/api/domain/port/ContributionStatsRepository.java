package com.meteomontana.api.domain.port;

import com.meteomontana.api.domain.model.SubmissionStatus;

import java.util.List;

/**
 * Estadísticas agregadas de contribuciones — puerto de solo lectura para
 * rankings (quién más ha aportado a la guía).
 */
public interface ContributionStatsRepository {

    /** uid del autor + nº de contribuciones suyas con ese status. */
    record ContributorCount(String uid, long count) {}

    /** Top de contribuidores con ese status, de más a menos, como mucho {@code limit}. */
    List<ContributorCount> topContributors(SubmissionStatus status, int limit);
}
