package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.AlertPreference;
import com.meteomontana.api.domain.port.AlertPreferenceRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class JpaAlertPreferenceRepositoryAdapter implements AlertPreferenceRepository {

    private final SpringDataWeekendAlertRepository jpaRepo;

    public JpaAlertPreferenceRepositoryAdapter(SpringDataWeekendAlertRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public List<AlertPreference> findEnabledFor(int notifyDay, int notifyHour) {
        return jpaRepo.findByEnabledTrueAndNotifyDayAndNotifyHour(notifyDay, notifyHour)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<AlertPreference> findOptimalEnabled() {
        return jpaRepo.findByOptimalEnabledTrue().stream().map(this::toDomain).toList();
    }

    @Override
    public void markOptimalSent(String uid, LocalDate day) {
        jpaRepo.findById(uid).ifPresent(e -> {
            e.setOptimalLastSent(day);
            jpaRepo.save(e);
        });
    }

    private AlertPreference toDomain(WeekendAlertPrefJpaEntity e) {
        return new AlertPreference(
                e.getUid(), e.isEnabled(), e.getNotifyDay(), e.getNotifyHour(),
                e.getSchoolIds(), e.getMode(), e.getRadiusKm(),
                e.getUserLat(), e.getUserLon(), e.getAlertDays(),
                e.isOptimalEnabled(), e.getOptimalThreshold(), e.getOptimalLastSent());
    }
}
