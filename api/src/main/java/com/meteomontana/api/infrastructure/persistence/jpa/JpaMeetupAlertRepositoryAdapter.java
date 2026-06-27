package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.MeetupAlert;
import com.meteomontana.api.domain.port.MeetupAlertRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaMeetupAlertRepositoryAdapter implements MeetupAlertRepository {

    private final SpringDataMeetupAlertRepository spring;

    public JpaMeetupAlertRepositoryAdapter(SpringDataMeetupAlertRepository spring) {
        this.spring = spring;
    }

    @Override
    public Optional<MeetupAlert> findByUidAndSchoolId(String uid, String schoolId) {
        return spring.findByUidAndSchoolId(uid, schoolId).map(this::toDomain);
    }

    @Override
    public List<MeetupAlert> findBySchoolId(String schoolId) {
        return spring.findBySchoolIdOrGlobal(schoolId).stream().map(this::toDomain).toList();
    }

    @Override
    public MeetupAlert save(MeetupAlert alert) {
        MeetupAlertJpaEntity entity = new MeetupAlertJpaEntity(
                alert.getId(), alert.getUid(), alert.getSchoolId(),
                alert.getDaysCsv(), alert.getCreatedAt()
        );
        return toDomain(spring.save(entity));
    }

    @Override
    @Transactional
    public void deleteByUidAndSchoolId(String uid, String schoolId) {
        spring.deleteByUidAndSchoolId(uid, schoolId);
    }

    private MeetupAlert toDomain(MeetupAlertJpaEntity e) {
        return new MeetupAlert(e.getId(), e.getUid(), e.getSchoolId(), e.getDaysCsv(), e.getCreatedAt());
    }
}
