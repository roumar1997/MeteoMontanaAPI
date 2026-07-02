package com.meteomontana.api.application.grips;

import com.meteomontana.api.domain.exception.GripNotFoundException;
import com.meteomontana.api.domain.exception.ForbiddenException;
import com.meteomontana.api.domain.model.GripMaxRecord;
import com.meteomontana.api.domain.model.GripMeasureSession;
import com.meteomontana.api.domain.model.GripWorkout;
import com.meteomontana.api.domain.model.GripWorkoutSet;
import com.meteomontana.api.domain.port.GripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class GripUseCase {

    private final GripRepository repository;

    public GripUseCase(GripRepository repository) {
        this.repository = repository;
    }

    public List<GripDtos.GripTypeDto> listTypes() {
        return repository.allGripTypes().stream().map(GripDtos.GripTypeDto::from).toList();
    }

    public List<GripDtos.GripMaxRecordDto> myMaxes(String uid) {
        return repository.maxRecordsFor(uid).stream().map(GripDtos.GripMaxRecordDto::from).toList();
    }

    /**
     * Guarda un máximo SOLO si supera al récord vigente para ese agarre+mano
     * (o si no había ninguno todavía). Si es menor, no se toca el récord — no
     * se avisa ni se bloquea, simplemente se conserva el mayor (decidido con
     * Rodrigo: puede que ese día estés más flojo, no hace falta interrumpir).
     */
    @Transactional
    public GripDtos.GripMaxRecordDto saveMaxIfHigher(String uid, GripDtos.SaveGripMaxRequest req) {
        validateHand(req.hand());
        repository.findGripType(req.gripTypeId())
                .orElseThrow(() -> new GripNotFoundException("Agarre no encontrado: " + req.gripTypeId()));

        var existing = repository.findMaxRecord(uid, req.gripTypeId(), req.hand());
        if (existing.isPresent() && existing.get().maxKg() >= req.kg()) {
            return GripDtos.GripMaxRecordDto.from(existing.get());
        }
        String id = existing.map(GripMaxRecord::id).orElseGet(() -> UUID.randomUUID().toString());
        GripMaxRecord saved = repository.saveMaxRecord(new GripMaxRecord(
                id, uid, req.gripTypeId(), req.hand(), req.kg(), req.edgeMm(), LocalDateTime.now()));
        return GripDtos.GripMaxRecordDto.from(saved);
    }

    public List<GripDtos.GripMeasureSessionDto> myMeasureSessions(String uid, Integer gripTypeId, String hand) {
        return repository.measureSessionsFor(uid, gripTypeId, hand).stream()
                .map(GripDtos.GripMeasureSessionDto::from).toList();
    }

    /**
     * Sube el resultado de un test de "Medir". Se guarda SIEMPRE en el
     * historial (para la gráfica de progreso, aunque no sea tu pico), y de
     * paso intenta actualizar el récord (solo si el pico supera al vigente).
     */
    @Transactional
    public GripDtos.GripMeasureSessionDto createMeasureSession(String uid, GripDtos.CreateGripMeasureSessionRequest req) {
        validateHand(req.hand());
        repository.findGripType(req.gripTypeId())
                .orElseThrow(() -> new GripNotFoundException("Agarre no encontrado: " + req.gripTypeId()));

        GripMeasureSession saved = repository.saveMeasureSession(new GripMeasureSession(
                UUID.randomUUID().toString(), uid, req.gripTypeId(), req.hand(),
                req.peakKg(), req.avgKg(), req.durationS(), req.edgeMm(), LocalDateTime.now()));

        saveMaxIfHigher(uid, new GripDtos.SaveGripMaxRequest(req.gripTypeId(), req.hand(), req.peakKg(), req.edgeMm()));

        return GripDtos.GripMeasureSessionDto.from(saved);
    }

    public List<GripDtos.GripWorkoutDto> myWorkouts(String uid) {
        return repository.workoutsFor(uid).stream().map(GripDtos.GripWorkoutDto::from).toList();
    }

    public GripDtos.GripWorkoutDto getWorkout(String uid, String id) {
        GripWorkout w = findOwnedWorkout(uid, id);
        return GripDtos.GripWorkoutDto.from(w);
    }

    @Transactional
    public GripDtos.GripWorkoutDto createWorkout(String uid, GripDtos.CreateGripWorkoutRequest req) {
        validateWorkoutRequest(req);
        LocalDateTime now = LocalDateTime.now();
        GripWorkout w = new GripWorkout(UUID.randomUUID().toString(), uid, req.name(),
                req.handMode(), req.countMode(), req.restBetweenSetsS(), now, now, toSets(req.sets()));
        return GripDtos.GripWorkoutDto.from(repository.saveWorkout(w));
    }

    @Transactional
    public GripDtos.GripWorkoutDto updateWorkout(String uid, String id, GripDtos.CreateGripWorkoutRequest req) {
        validateWorkoutRequest(req);
        GripWorkout current = findOwnedWorkout(uid, id);
        GripWorkout updated = new GripWorkout(current.getId(), uid, req.name(), req.handMode(), req.countMode(),
                req.restBetweenSetsS(), current.getCreatedAt(), LocalDateTime.now(), toSets(req.sets()));
        return GripDtos.GripWorkoutDto.from(repository.saveWorkout(updated));
    }

    @Transactional
    public void deleteWorkout(String uid, String id) {
        findOwnedWorkout(uid, id);
        repository.deleteWorkout(id);
    }

    private GripWorkout findOwnedWorkout(String uid, String id) {
        GripWorkout w = repository.findWorkout(id)
                .orElseThrow(() -> new GripNotFoundException("Entreno no encontrado: " + id));
        if (!w.getUid().equals(uid)) {
            throw new ForbiddenException("No es tu entreno");
        }
        return w;
    }

    private List<GripWorkoutSet> toSets(List<GripDtos.GripWorkoutSetRequest> sets) {
        return sets.stream().map(s -> new GripWorkoutSet(
                UUID.randomUUID().toString(), s.sortOrder(), s.reps(), s.workS(), s.restS(),
                s.gripTypeId(), s.targetMinPct(), s.targetMaxPct())).toList();
    }

    private void validateHand(String hand) {
        if (!"LEFT".equals(hand) && !"RIGHT".equals(hand)) {
            throw new IllegalArgumentException("hand debe ser LEFT o RIGHT: " + hand);
        }
    }

    private void validateWorkoutRequest(GripDtos.CreateGripWorkoutRequest req) {
        if (req.name() == null || req.name().isBlank()) {
            throw new IllegalArgumentException("El entreno necesita un nombre");
        }
        if (!List.of("UNA", "POR_SERIE", "POR_REP").contains(req.handMode())) {
            throw new IllegalArgumentException("handMode inválido: " + req.handMode());
        }
        if (!List.of("TIEMPO", "PESO").contains(req.countMode())) {
            throw new IllegalArgumentException("countMode inválido: " + req.countMode());
        }
        if (req.sets() == null || req.sets().isEmpty()) {
            throw new IllegalArgumentException("El entreno necesita al menos un set");
        }
        for (var s : req.sets()) {
            if (s.targetMinPct() > s.targetMaxPct()) {
                throw new IllegalArgumentException("targetMinPct no puede ser mayor que targetMaxPct");
            }
            repository.findGripType(s.gripTypeId())
                    .orElseThrow(() -> new GripNotFoundException("Agarre no encontrado: " + s.gripTypeId()));
        }
    }
}
