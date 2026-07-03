package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataUserDeviceRepository extends JpaRepository<UserDeviceJpaEntity, String> {
    List<UserDeviceJpaEntity> findByUid(String uid);
}
