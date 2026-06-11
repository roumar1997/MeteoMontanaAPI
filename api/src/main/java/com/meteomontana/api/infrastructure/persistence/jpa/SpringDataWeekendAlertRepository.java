package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataWeekendAlertRepository
        extends JpaRepository<WeekendAlertPrefJpaEntity, String> {

    List<WeekendAlertPrefJpaEntity> findByEnabledTrueAndNotifyDayAndNotifyHour(int notifyDay, int notifyHour);
}
