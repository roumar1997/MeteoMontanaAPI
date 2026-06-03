package com.meteomontana.api.domain.port;

import com.meteomontana.api.domain.model.School;
import java.util.List;
import java.util.Optional;

public interface SchoolRepository {
    List<School> findAll();
    Optional<School> findById(String id);
    School save(School school);
}
