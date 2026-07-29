package com.meteomontana.api.application.admin;

import com.meteomontana.api.domain.model.AdminLog;
import com.meteomontana.api.domain.port.AdminLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListAdminLogsUseCase {

    private final AdminLogRepository repository;
    private final AdminGuard adminGuard;

    public List<AdminLog> execute(String adminUid, int limit) {
        adminGuard.ensureAdmin(adminUid);
        return repository.findRecent(Math.min(Math.max(limit, 1), 500));
    }
}
