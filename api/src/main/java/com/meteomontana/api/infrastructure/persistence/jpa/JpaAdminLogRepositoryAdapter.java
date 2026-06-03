package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.AdminLog;
import com.meteomontana.api.domain.port.AdminLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JpaAdminLogRepositoryAdapter implements AdminLogRepository {

    private final SpringDataAdminLogRepository jpaRepo;

    public JpaAdminLogRepositoryAdapter(SpringDataAdminLogRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public AdminLog save(AdminLog log) {
        AdminLogJpaEntity e = new AdminLogJpaEntity(
                log.getId(), log.getActorUid(), log.getAction(),
                log.getTargetType(), log.getTargetId(), log.getDetails(),
                log.getCreatedAt()
        );
        return toDomain(jpaRepo.save(e));
    }

    @Override
    public List<AdminLog> findRecent(int limit) {
        return jpaRepo.findRecent(PageRequest.of(0, limit)).stream()
                .map(this::toDomain).toList();
    }

    private AdminLog toDomain(AdminLogJpaEntity e) {
        return new AdminLog(
                e.getId(), e.getActorUid(), e.getAction(),
                e.getTargetType(), e.getTargetId(), e.getDetails(),
                e.getCreatedAt()
        );
    }
}
