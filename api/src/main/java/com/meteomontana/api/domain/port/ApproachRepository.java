package com.meteomontana.api.domain.port;

import com.meteomontana.api.domain.model.Approach;
import com.meteomontana.api.domain.model.ApproachPin;
import java.util.List;
import java.util.Optional;

public interface ApproachRepository {
    List<Approach> findBySchoolId(String schoolId);
    Optional<Approach> findById(String id);
    Approach save(Approach approach);
    ApproachPin addPin(String approachId, ApproachPin pin);
    void deleteApproach(String id);
    void deletePin(String pinId);
}
