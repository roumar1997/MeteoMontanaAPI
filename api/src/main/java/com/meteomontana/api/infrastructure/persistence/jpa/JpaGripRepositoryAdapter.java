package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.GripMaxRecord;
import com.meteomontana.api.domain.model.GripMeasureSession;
import com.meteomontana.api.domain.model.GripType;
import com.meteomontana.api.domain.model.GripWorkout;
import com.meteomontana.api.domain.model.GripWorkoutSet;
import com.meteomontana.api.domain.port.GripRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaGripRepositoryAdapter implements GripRepository {

    private final SpringDataGripTypeRepository typeRepo;
    private final SpringDataGripMaxRecordRepository maxRepo;
    private final SpringDataGripMeasureSessionRepository measureRepo;
    private final SpringDataGripWorkoutRepository workoutRepo;
    private final SpringDataGripWorkoutSetRepository setRepo;

    public JpaGripRepositoryAdapter(SpringDataGripTypeRepository typeRepo,
                                     SpringDataGripMaxRecordRepository maxRepo,
                                     SpringDataGripMeasureSessionRepository measureRepo,
                                     SpringDataGripWorkoutRepository workoutRepo,
                                     SpringDataGripWorkoutSetRepository setRepo) {
        this.typeRepo = typeRepo;
        this.maxRepo = maxRepo;
        this.measureRepo = measureRepo;
        this.workoutRepo = workoutRepo;
        this.setRepo = setRepo;
    }

    // ---- Grip types ----

    @Override
    public List<GripType> allGripTypes() {
        return typeRepo.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<GripType> findGripType(int id) {
        return typeRepo.findById(id).map(this::toDomain);
    }

    private GripType toDomain(GripTypeJpaEntity e) {
        return new GripType(e.getId(), e.getFingerGroup(), e.getStyle());
    }

    // ---- Max records ----

    @Override
    public List<GripMaxRecord> maxRecordsFor(String uid) {
        return maxRepo.findByUid(uid).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<GripMaxRecord> findMaxRecord(String uid, int gripTypeId, String hand) {
        return maxRepo.findByUidAndGripTypeIdAndHand(uid, gripTypeId, hand).map(this::toDomain);
    }

    @Override
    public GripMaxRecord saveMaxRecord(GripMaxRecord r) {
        GripMaxRecordJpaEntity e = new GripMaxRecordJpaEntity(
                r.id(), r.uid(), r.gripTypeId(), r.hand(), r.maxKg(), r.edgeMm(), r.measuredAt());
        return toDomain(maxRepo.save(e));
    }

    private GripMaxRecord toDomain(GripMaxRecordJpaEntity e) {
        return new GripMaxRecord(e.getId(), e.getUid(), e.getGripTypeId(), e.getHand(),
                e.getMaxKg(), e.getEdgeMm(), e.getMeasuredAt());
    }

    // ---- Measure sessions (historial) ----

    @Override
    public List<GripMeasureSession> measureSessionsFor(String uid, Integer gripTypeId, String hand) {
        List<GripMeasureSessionJpaEntity> list = (gripTypeId != null && hand != null)
                ? measureRepo.findByUidAndGripTypeIdAndHandOrderByCreatedAtDesc(uid, gripTypeId, hand)
                : measureRepo.findByUidOrderByCreatedAtDesc(uid);
        return list.stream().map(this::toDomain).toList();
    }

    @Override
    public GripMeasureSession saveMeasureSession(GripMeasureSession s) {
        GripMeasureSessionJpaEntity e = new GripMeasureSessionJpaEntity(
                s.id(), s.uid(), s.gripTypeId(), s.hand(), s.peakKg(), s.avgKg(),
                s.durationS(), s.edgeMm(), s.createdAt());
        return toDomain(measureRepo.save(e));
    }

    private GripMeasureSession toDomain(GripMeasureSessionJpaEntity e) {
        return new GripMeasureSession(e.getId(), e.getUid(), e.getGripTypeId(), e.getHand(),
                e.getPeakKg(), e.getAvgKg(), e.getDurationS(), e.getEdgeMm(), e.getCreatedAt());
    }

    // ---- Workouts (plantillas) ----

    @Override
    public List<GripWorkout> workoutsFor(String uid) {
        return workoutRepo.findByUidOrderByUpdatedAtDesc(uid).stream().map(this::toDomainWithSets).toList();
    }

    @Override
    public Optional<GripWorkout> findWorkout(String id) {
        return workoutRepo.findById(id).map(this::toDomainWithSets);
    }

    @Override
    @Transactional
    public GripWorkout saveWorkout(GripWorkout w) {
        GripWorkoutJpaEntity e = new GripWorkoutJpaEntity(
                w.getId(), w.getUid(), w.getName(), w.getHandMode(), w.getCountMode(),
                w.getRestBetweenSetsS(), w.getCreatedAt(), w.getUpdatedAt());
        workoutRepo.save(e);
        // Los sets se reemplazan enteros en cada guardado (más simple que hacer diff).
        setRepo.deleteByWorkoutId(w.getId());
        setRepo.flush();
        for (GripWorkoutSet s : w.getSets()) {
            setRepo.save(new GripWorkoutSetJpaEntity(
                    s.id(), w.getId(), s.sortOrder(), s.reps(), s.workS(), s.restS(),
                    s.gripTypeId(), s.targetMinPct(), s.targetMaxPct()));
        }
        return findWorkout(w.getId()).orElseThrow();
    }

    @Override
    public void deleteWorkout(String id) {
        workoutRepo.deleteById(id);
    }

    private GripWorkout toDomainWithSets(GripWorkoutJpaEntity e) {
        List<GripWorkoutSet> sets = setRepo.findByWorkoutIdOrderBySortOrder(e.getId()).stream()
                .map(s -> new GripWorkoutSet(s.getId(), s.getSortOrder(), s.getReps(), s.getWorkS(),
                        s.getRestS(), s.getGripTypeId(), s.getTargetMinPct(), s.getTargetMaxPct()))
                .toList();
        return new GripWorkout(e.getId(), e.getUid(), e.getName(), e.getHandMode(), e.getCountMode(),
                e.getRestBetweenSetsS(), e.getCreatedAt(), e.getUpdatedAt(), sets);
    }
}
