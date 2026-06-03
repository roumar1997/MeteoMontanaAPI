package com.meteomontana.api.application.submissions;

import com.meteomontana.api.domain.model.SchoolSubmission;
import com.meteomontana.api.domain.model.SubmissionStatus;
import com.meteomontana.api.domain.port.SchoolSubmissionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SubmitSchoolUseCase {

    private final SchoolSubmissionRepository repository;

    public SubmitSchoolUseCase(SchoolSubmissionRepository repository) {
        this.repository = repository;
    }

    public SubmissionDto execute(String submitterUid, SubmitSchoolRequest req) {
        if (req.name() == null || req.name().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (req.lat() == null || req.lon() == null) {
            throw new IllegalArgumentException("lat and lon are required");
        }
        if (req.lat() < -90 || req.lat() > 90 || req.lon() < -180 || req.lon() > 180) {
            throw new IllegalArgumentException("Invalid coordinates");
        }

        SchoolSubmission s = new SchoolSubmission(
                UUID.randomUUID().toString(),
                req.name().trim(),
                req.region(),
                req.style(),
                req.rockType(),
                req.lat(),
                req.lon(),
                req.location(),
                req.source(),
                req.notes(),
                SubmissionStatus.PENDING,
                submitterUid,
                null, null, null,
                LocalDateTime.now(),
                null
        );
        return SubmissionDto.from(repository.save(s));
    }
}
