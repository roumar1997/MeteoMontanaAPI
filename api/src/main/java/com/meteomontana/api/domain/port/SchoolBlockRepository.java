package com.meteomontana.api.domain.port;

import com.meteomontana.api.domain.model.SchoolBlock;

import java.util.List;
import java.util.Optional;

public interface SchoolBlockRepository {
    SchoolBlock save(SchoolBlock block);
    Optional<SchoolBlock> findById(String id);
    List<SchoolBlock> findBySchoolId(String schoolId);
    void deleteById(String id);
}
