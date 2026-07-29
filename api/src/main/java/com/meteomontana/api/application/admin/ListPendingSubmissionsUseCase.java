package com.meteomontana.api.application.admin;

import com.meteomontana.api.application.submissions.SubmissionDto;
import com.meteomontana.api.domain.model.SubmissionStatus;
import com.meteomontana.api.domain.port.SchoolSubmissionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListPendingSubmissionsUseCase {

    private final SchoolSubmissionRepository repository;
    private final AdminGuard adminGuard;

    public List<SubmissionDto> execute(String adminUid) {
        adminGuard.ensureAdmin(adminUid);
        return repository.findByStatus(SubmissionStatus.PENDING).stream()
                .map(SubmissionDto::from)
                .toList();
    }
}
