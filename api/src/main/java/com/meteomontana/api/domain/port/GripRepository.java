package com.meteomontana.api.domain.port;

import com.meteomontana.api.domain.model.GripMaxRecord;
import com.meteomontana.api.domain.model.GripMeasureSession;
import com.meteomontana.api.domain.model.GripType;
import com.meteomontana.api.domain.model.GripWorkout;

import java.util.List;
import java.util.Optional;

public interface GripRepository {
    List<GripType> allGripTypes();
    Optional<GripType> findGripType(int id);

    List<GripMaxRecord> maxRecordsFor(String uid);
    Optional<GripMaxRecord> findMaxRecord(String uid, int gripTypeId, String hand);
    GripMaxRecord saveMaxRecord(GripMaxRecord record);

    List<GripMeasureSession> measureSessionsFor(String uid, Integer gripTypeId, String hand);
    GripMeasureSession saveMeasureSession(GripMeasureSession session);

    List<GripWorkout> workoutsFor(String uid);
    Optional<GripWorkout> findWorkout(String id);
    GripWorkout saveWorkout(GripWorkout workout);
    void deleteWorkout(String id);
}
