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
    public java.util.Optional<AlertPreference> findByUid(String uid) {
        return jpaRepo.findById(uid).map(this::toDomain);
    }

    @Override
    public void save(AlertPreference p) {
        var entity = jpaRepo.findById(p.uid())
                .orElse(new WeekendAlertPrefJpaEntity(p.uid(), true, 4, 20, "",
                        java.time.LocalDateTime.now()));
        entity.setEnabled(p.enabled());
        entity.setNotifyDay(p.notifyDay());
        entity.setNotifyHour(p.notifyHour());
        entity.setMode(p.mode());
        entity.setRadiusKm(p.radiusKm());
        entity.setUserLat(p.userLat());
        entity.setUserLon(p.userLon());
        entity.setSchoolIds(p.schoolIds());
        entity.setAlertDays(p.alertDays());
        entity.setOptimalEnabled(p.optimalEnabled());
        entity.setOptimalThreshold(p.optimalThreshold());
        entity.setUpdatedAt(java.time.LocalDateTime.now());
        jpaRepo.save(entity);
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
