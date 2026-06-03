package com.meteomontana.api.domain.port;

import com.meteomontana.api.domain.model.SchoolSubmission;
import com.meteomontana.api.domain.model.SubmissionStatus;

import java.util.List;
import java.util.Optional;

public interface SchoolSubmissionRepository {
    SchoolSubmission save(SchoolSubmission submission);
    Optional<SchoolSubmission> findById(String id);
    List<SchoolSubmission> findByStatus(SubmissionStatus status);
    List<SchoolSubmission> findBySubmittedByUid(String uid);
}
