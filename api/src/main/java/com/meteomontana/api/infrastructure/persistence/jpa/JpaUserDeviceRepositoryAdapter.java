package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.port.UserDeviceRepository;
import org.springframework.stereotype.Repository;

@Repository
public class JpaUserDeviceRepositoryAdapter implements UserDeviceRepository {

    private final SpringDataUserDeviceRepository jpaRepo;

    public JpaUserDeviceRepositoryAdapter(SpringDataUserDeviceRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public java.util.List<String> allTokens() {
        return jpaRepo.findAll().stream().map(UserDeviceJpaEntity::getToken).toList();
    }

    @Override
    public void registerDevice(String token, String uid) {
        UserDeviceJpaEntity device = jpaRepo.findById(token)
                .orElseGet(() -> new UserDeviceJpaEntity(token, uid));
        device.setUid(uid);
        jpaRepo.save(device);
    }
}
