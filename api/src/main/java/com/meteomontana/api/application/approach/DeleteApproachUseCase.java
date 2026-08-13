package com.meteomontana.api.application.approach;

import com.meteomontana.api.application.admin.AdminGuard;
import com.meteomontana.api.domain.port.ApproachRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteApproachUseCase {
    private final ApproachRepository repository;
    private final AdminGuard adminGuard;

    public void delete(String adminUid, String approachId) {
        adminGuard.ensureAdmin(adminUid);
        repository.deleteApproach(approachId);
    }
}
