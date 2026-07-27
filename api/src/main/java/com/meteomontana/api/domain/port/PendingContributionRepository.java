package com.meteomontana.api.domain.port;

import com.meteomontana.api.domain.model.PendingContribution;

/**
 * Cola de contribuciones pendientes de revisión (puerto de dominio).
 *
 * Ojo: la APROBACIÓN (materializar la propuesta en la piedra) sigue trabajando
 * con entidades JPA a propósito — ver la nota de ArchitectureTest: depende de
 * la identidad y el dirty-checking de Hibernate para PRESERVAR los ids de las
 * vías, que es lo que mantiene vivos los enganches del diario.
 */
public interface PendingContributionRepository {
    /** Guarda una propuesta nueva y devuelve su id. */
    String save(PendingContribution contribution);
}
