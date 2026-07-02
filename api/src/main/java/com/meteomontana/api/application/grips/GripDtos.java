package com.meteomontana.api.application.grips;

import com.meteomontana.api.domain.model.GripMaxRecord;
import com.meteomontana.api.domain.model.GripMeasureSession;
import com.meteomontana.api.domain.model.GripType;
import com.meteomontana.api.domain.model.GripWorkout;
import com.meteomontana.api.domain.model.GripWorkoutSet;

import java.time.LocalDateTime;
import java.util.List;

public class GripDtos {

    public record GripTypeDto(int id, String fingerGroup, String style) {
        public static GripTypeDto from(GripType t) {
            return new GripTypeDto(t.id(), t.fingerGroup(), t.style());
        }
    }

    public record GripMaxRecordDto(
            String id, int gripTypeId, String hand, double maxKg, String edgeMm, LocalDateTime measuredAt
    ) {
        public static GripMaxRecordDto from(GripMaxRecord r) {
            return new GripMaxRecordDto(r.id(), r.gripTypeId(), r.hand(), r.maxKg(), r.edgeMm(), r.measuredAt());
        }
    }

    public record SaveGripMaxRequest(int gripTypeId, String hand, double kg, String edgeMm) {}

    public record GripMeasureSessionDto(
            String id, int gripTypeId, String hand, double peakKg, double avgKg,
            int durationS, String edgeMm, LocalDateTime createdAt
    ) {
        public static GripMeasureSessionDto from(GripMeasureSession s) {
            return new GripMeasureSessionDto(s.id(), s.gripTypeId(), s.hand(), s.peakKg(), s.avgKg(),
                    s.durationS(), s.edgeMm(), s.createdAt());
        }
    }

    public record CreateGripMeasureSessionRequest(
            int gripTypeId, String hand, double peakKg, double avgKg, int durationS, String edgeMm
    ) {}

    public record GripWorkoutSetDto(
            String id, int sortOrder, int reps, int workS, int restS,
            int gripTypeId, double targetMinPct, double targetMaxPct
    ) {
        public static GripWorkoutSetDto from(GripWorkoutSet s) {
            return new GripWorkoutSetDto(s.id(), s.sortOrder(), s.reps(), s.workS(), s.restS(),
                    s.gripTypeId(), s.targetMinPct(), s.targetMaxPct());
        }
    }

    public record GripWorkoutSetRequest(
            int sortOrder, int reps, int workS, int restS,
            int gripTypeId, double targetMinPct, double targetMaxPct
    ) {}

    public record GripWorkoutDto(
            String id, String name, String handMode, String countMode, int restBetweenSetsS,
            LocalDateTime createdAt, LocalDateTime updatedAt, List<GripWorkoutSetDto> sets
    ) {
        public static GripWorkoutDto from(GripWorkout w) {
            return new GripWorkoutDto(w.getId(), w.getName(), w.getHandMode(), w.getCountMode(),
                    w.getRestBetweenSetsS(), w.getCreatedAt(), w.getUpdatedAt(),
                    w.getSets().stream().map(GripWorkoutSetDto::from).toList());
        }
    }

    public record CreateGripWorkoutRequest(
            String name, String handMode, String countMode, int restBetweenSetsS,
            List<GripWorkoutSetRequest> sets
    ) {}

    private GripDtos() {}
}
