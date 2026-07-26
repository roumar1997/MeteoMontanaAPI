package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.port.UserBlockRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class JpaUserBlockRepositoryAdapter implements UserBlockRepository {

    private final SpringDataUserBlockRepository jpaRepo;

    public JpaUserBlockRepositoryAdapter(SpringDataUserBlockRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public Set<String> blockedUidsOf(String blockerUid) {
        return jpaRepo.findByBlockerUid(blockerUid).stream()
                .map(UserBlockJpaEntity::getBlockedUid).collect(Collectors.toSet());
    }

    @Override
    public boolean isBlocked(String blockerUid, String blockedUid) {
        return jpaRepo.existsByBlockerUidAndBlockedUid(blockerUid, blockedUid);
    }
}
