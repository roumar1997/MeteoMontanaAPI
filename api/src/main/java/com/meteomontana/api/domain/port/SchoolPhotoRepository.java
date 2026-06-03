package com.meteomontana.api.domain.port;

import com.meteomontana.api.domain.model.SchoolPhoto;

import java.util.List;
import java.util.Optional;

public interface SchoolPhotoRepository {
    List<SchoolPhoto> findBySchoolId(String schoolId);
    Optional<SchoolPhoto> findById(String id);
    SchoolPhoto save(SchoolPhoto photo);
    void deleteById(String id);
}
