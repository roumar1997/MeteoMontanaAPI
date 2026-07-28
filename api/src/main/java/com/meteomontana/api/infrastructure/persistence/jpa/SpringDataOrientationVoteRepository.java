package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.infrastructure.persistence.jpa.CommunityVoteJpaEntities.OrientationVoteJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * TOP-LEVEL a proposito: anidada dentro del adaptador, el escaneo de
 * repositorios NO la registraba y el contexto no arrancaba (cazado por el
 * CI el 2026-07-28 - staging llego a caerse por esto).
 */
public interface SpringDataOrientationVoteRepository
        extends JpaRepository<OrientationVoteJpaEntity, String> {
    List<OrientationVoteJpaEntity> findByBlockId(String blockId);
}
