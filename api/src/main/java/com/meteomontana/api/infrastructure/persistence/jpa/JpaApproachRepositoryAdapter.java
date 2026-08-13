package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.Approach;
import com.meteomontana.api.domain.model.ApproachPin;
import com.meteomontana.api.domain.port.ApproachRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaApproachRepositoryAdapter implements ApproachRepository {

    private final SpringDataApproachRepository jpa;

    @Override
    public List<Approach> findBySchoolId(String schoolId) {
        return jpa.findBySchoolId(schoolId).stream().map(this::toDomain).toList();
    }

    private Approach toDomain(ApproachJpaEntity e) {
        List<ApproachPin> pins = e.getPins().stream().map(this::toDomain).toList();
        return new Approach(
                e.getId(), e.getSchoolId(), e.getFromBlockId(), e.getToBlockId(),
                e.getName(), e.getPathJson(), e.getDistanceM(), e.getAscentM(),
                e.getDurationMin(), e.getSource(), e.getStatus(), e.getAuthorUid(),
                e.getCreatedAt(), pins);
    }

    private ApproachPin toDomain(ApproachPinJpaEntity e) {
        return new ApproachPin(
                e.getId(), e.getApproach().getId(), e.getLat(), e.getLon(), e.getPositionIdx(),
                e.getKind(), e.getMessage(), e.getPhotoPath(), e.getAuthorUid(),
                e.getStatus(), e.getCreatedAt());
    }
}
