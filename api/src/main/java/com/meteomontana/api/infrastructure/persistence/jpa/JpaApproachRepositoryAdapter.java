package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.exception.SchoolNotFoundException;
import com.meteomontana.api.domain.model.Approach;
import com.meteomontana.api.domain.model.ApproachPin;
import com.meteomontana.api.domain.port.ApproachRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaApproachRepositoryAdapter implements ApproachRepository {

    private final SpringDataApproachRepository jpa;
    private final SpringDataApproachPinRepository pinJpa;

    @Override
    public List<Approach> findBySchoolId(String schoolId) {
        return jpa.findBySchoolId(schoolId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Approach> findById(String id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Approach save(Approach a) {
        ApproachJpaEntity entity = jpa.findById(a.getId()).orElseGet(() -> new ApproachJpaEntity(
                a.getId(), a.getSchoolId(), a.getFromBlockId(), a.getToBlockId(),
                a.getName(), a.getPathJson(), a.getDistanceM(), a.getAscentM(),
                a.getDurationMin(), a.getSource(), a.getStatus(), a.getAuthorUid(),
                LocalDateTime.now()));
        // Los campos mutables (nombre/estado al verificar, etc.) se vuelcan aparte:
        // el constructor solo sirve para el alta; una fila existente se actualiza.
        ApproachJpaEntity saved = jpa.save(entity);
        return toDomain(saved);
    }

    @Override
    public ApproachPin addPin(String approachId, ApproachPin pin) {
        ApproachJpaEntity approach = jpa.findById(approachId)
                .orElseThrow(() -> new SchoolNotFoundException(approachId));
        ApproachPinJpaEntity entity = new ApproachPinJpaEntity(
                pin.getId(), pin.getLat(), pin.getLon(), pin.getPositionIdx(),
                pin.getKind(), pin.getMessage(), pin.getPhotoPath(), pin.getAuthorUid(),
                pin.getStatus(), LocalDateTime.now());
        approach.addPin(entity);
        jpa.save(approach);
        return toDomain(entity);
    }

    @Override
    public void deleteApproach(String id) {
        jpa.deleteById(id);
    }

    @Override
    public void deletePin(String pinId) {
        pinJpa.deleteById(pinId);
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
