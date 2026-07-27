package com.meteomontana.api.domain.port;

import com.meteomontana.api.domain.model.SchoolBlock;

import java.util.List;
import java.util.Optional;

public interface SchoolBlockRepository {
    SchoolBlock save(SchoolBlock block);
    Optional<SchoolBlock> findById(String id);
    /** La piedra/muro que contiene una vía concreta (landings /s/v). */
    Optional<SchoolBlock> findByLineId(String lineId);
    List<SchoolBlock> findBySchoolId(String schoolId);
    /** Carga en batch por ids (una query por página del feed, sin N+1). */
    List<SchoolBlock> findByIds(java.util.Collection<String> ids);
    void deleteById(String id);
}
