package com.meteomontana.api.domain.port;

import com.meteomontana.api.domain.model.Approach;
import java.util.List;

public interface ApproachRepository {
    List<Approach> findBySchoolId(String schoolId);
}
