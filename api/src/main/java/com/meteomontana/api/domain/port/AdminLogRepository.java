package com.meteomontana.api.domain.port;

import com.meteomontana.api.domain.model.AdminLog;

import java.util.List;

public interface AdminLogRepository {
    AdminLog save(AdminLog log);
    List<AdminLog> findRecent(int limit);
}
