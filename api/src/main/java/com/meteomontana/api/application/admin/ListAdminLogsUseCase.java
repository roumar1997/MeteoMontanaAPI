package com.meteomontana.api.application.admin;

import com.meteomontana.api.domain.model.AdminLog;
import com.meteomontana.api.domain.port.AdminLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListAdminLogsUseCase {

    private final AdminLogRepository repository;
    private final AdminGuard adminGuard;

    public ListAdminLogsUseCase(AdminLogRepository repository, AdminGuard adminGuard) {
        this.repository = repository;
        this.adminGuard = adminGuard;
    }

    public List<AdminLog> execute(String adminUid, int limit) {
        adminGuard.ensureAdmin(adminUid);
        return repository.findRecent(Math.min(Math.max(limit, 1), 500));
    }
}
