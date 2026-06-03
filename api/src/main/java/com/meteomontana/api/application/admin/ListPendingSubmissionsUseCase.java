package com.meteomontana.api.application.admin;

import com.meteomontana.api.application.submissions.SubmissionDto;
import com.meteomontana.api.domain.model.SubmissionStatus;
import com.meteomontana.api.domain.port.SchoolSubmissionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListPendingSubmissionsUseCase {

    private final SchoolSubmissionRepository repository;
    private final AdminGuard adminGuard;

    public ListPendingSubmissionsUseCase(SchoolSubmissionRepository repository,
                                         AdminGuard adminGuard) {
        this.repository = repository;
        this.adminGuard = adminGuard;
    }

    public List<SubmissionDto> execute(String adminUid) {
        adminGuard.ensureAdmin(adminUid);
        return repository.findByStatus(SubmissionStatus.PENDING).stream()
                .map(SubmissionDto::from)
                .toList();
    }
}
