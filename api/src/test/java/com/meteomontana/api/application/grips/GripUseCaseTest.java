package com.meteomontana.api.application.grips;

import com.meteomontana.api.domain.exception.GripNotFoundException;
import com.meteomontana.api.domain.exception.ForbiddenException;
import com.meteomontana.api.domain.model.GripMaxRecord;
import com.meteomontana.api.domain.model.GripType;
import com.meteomontana.api.domain.model.GripWorkout;
import com.meteomontana.api.domain.model.GripWorkoutSet;
import com.meteomontana.api.domain.port.GripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GripUseCaseTest {

    GripRepository repository = mock(GripRepository.class);
    GripUseCase useCase;

    private static final String UID = "uid-1";
    private static final GripType FOUR_HALF_CRIMP = new GripType(4, "FOUR", "HALF_CRIMP");

    @BeforeEach void setUp() {
        useCase = new GripUseCase(repository);
        when(repository.findGripType(4)).thenReturn(Optional.of(FOUR_HALF_CRIMP));
    }

    @Test void saveMax_stores_first_record() {
        when(repository.findMaxRecord(UID, 4, "LEFT")).thenReturn(Optional.empty());
        when(repository.saveMaxRecord(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = useCase.saveMaxIfHigher(UID, new GripDtos.SaveGripMaxRequest(4, "LEFT", 18.2, "20mm"));

        assertThat(result.maxKg()).isEqualTo(18.2);
        verify(repository).saveMaxRecord(any());
    }

    @Test void saveMax_does_NOT_overwrite_when_lower_than_existing() {
        var existing = new GripMaxRecord("rec-1", UID, 4, "LEFT", 20.0, "20mm", LocalDateTime.now());
        when(repository.findMaxRecord(UID, 4, "LEFT")).thenReturn(Optional.of(existing));

        var result = useCase.saveMaxIfHigher(UID, new GripDtos.SaveGripMaxRequest(4, "LEFT", 15.0, "20mm"));

        // Sigue siendo el récord vigente (20.0), no se sobrescribe con el valor menor.
        assertThat(result.maxKg()).isEqualTo(20.0);
        verify(repository, org.mockito.Mockito.never()).saveMaxRecord(any());
    }

    @Test void saveMax_overwrites_when_higher_than_existing() {
        var existing = new GripMaxRecord("rec-1", UID, 4, "LEFT", 20.0, "20mm", LocalDateTime.now());
        when(repository.findMaxRecord(UID, 4, "LEFT")).thenReturn(Optional.of(existing));
        when(repository.saveMaxRecord(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = useCase.saveMaxIfHigher(UID, new GripDtos.SaveGripMaxRequest(4, "LEFT", 22.5, "20mm"));

        assertThat(result.maxKg()).isEqualTo(22.5);
        verify(repository).saveMaxRecord(any());
    }

    @Test void saveMax_rejects_unknown_grip_type() {
        when(repository.findGripType(999)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.saveMaxIfHigher(UID, new GripDtos.SaveGripMaxRequest(999, "LEFT", 10, null)))
                .isInstanceOf(GripNotFoundException.class);
    }

    @Test void saveMax_rejects_invalid_hand() {
        assertThatThrownBy(() -> useCase.saveMaxIfHigher(UID, new GripDtos.SaveGripMaxRequest(4, "BOTH", 10, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void createMeasureSession_saves_history_and_updates_max_if_higher() {
        when(repository.findMaxRecord(UID, 4, "LEFT")).thenReturn(Optional.empty());
        when(repository.saveMeasureSession(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.saveMaxRecord(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new GripDtos.CreateGripMeasureSessionRequest(4, "LEFT", 19.0, 17.5, 8, "20mm");
        var result = useCase.createMeasureSession(UID, req);

        assertThat(result.peakKg()).isEqualTo(19.0);
        verify(repository).saveMeasureSession(any());
        verify(repository).saveMaxRecord(any());
    }

    @Test void createWorkout_rejects_inverted_target_range() {
        var badSet = new GripDtos.GripWorkoutSetRequest(1, 6, 10, 20, 4, 30.0, 10.0);
        var req = new GripDtos.CreateGripWorkoutRequest("Test", "POR_REP", "TIEMPO", 30, List.of(badSet));

        assertThatThrownBy(() -> useCase.createWorkout(UID, req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void createWorkout_rejects_empty_sets() {
        var req = new GripDtos.CreateGripWorkoutRequest("Test", "POR_REP", "TIEMPO", 30, List.of());
        assertThatThrownBy(() -> useCase.createWorkout(UID, req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void updateWorkout_rejects_when_not_owner() {
        var otherUsersWorkout = new GripWorkout("w-1", "other-uid", "Test", "POR_REP", "TIEMPO",
                30, LocalDateTime.now(), LocalDateTime.now(), List.<GripWorkoutSet>of());
        when(repository.findWorkout("w-1")).thenReturn(Optional.of(otherUsersWorkout));

        var goodSet = new GripDtos.GripWorkoutSetRequest(1, 6, 10, 20, 4, 10.0, 30.0);
        var req = new GripDtos.CreateGripWorkoutRequest("Test", "POR_REP", "TIEMPO", 30, List.of(goodSet));

        assertThatThrownBy(() -> useCase.updateWorkout(UID, "w-1", req))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test void deleteWorkout_rejects_when_not_found() {
        when(repository.findWorkout("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.deleteWorkout(UID, "missing"))
                .isInstanceOf(GripNotFoundException.class);
    }
}
