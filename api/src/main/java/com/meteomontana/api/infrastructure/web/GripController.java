package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.grips.GripDtos;
import com.meteomontana.api.application.grips.GripUseCase;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Pestaña "Agarres" — dinamómetro BLE, máximos, historial y entrenos. Ver GRIPS_DESIGN.md. */
@RestController
@RequestMapping("/api")
public class GripController {

    private final GripUseCase useCase;

    public GripController(GripUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/grips/types")
    public List<GripDtos.GripTypeDto> types() {
        return useCase.listTypes();
    }

    @GetMapping("/me/grip-maxes")
    public List<GripDtos.GripMaxRecordDto> myMaxes(@AuthenticationPrincipal FirebaseUser user) {
        return useCase.myMaxes(user.uid());
    }

    @PostMapping("/me/grip-maxes")
    public GripDtos.GripMaxRecordDto saveMax(@AuthenticationPrincipal FirebaseUser user,
                                              @RequestBody GripDtos.SaveGripMaxRequest req) {
        return useCase.saveMaxIfHigher(user.uid(), req);
    }

    @GetMapping("/me/grip-measure-sessions")
    public List<GripDtos.GripMeasureSessionDto> myMeasureSessions(
            @AuthenticationPrincipal FirebaseUser user,
            @RequestParam(required = false) Integer gripTypeId,
            @RequestParam(required = false) String hand) {
        return useCase.myMeasureSessions(user.uid(), gripTypeId, hand);
    }

    @PostMapping("/me/grip-measure-sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public GripDtos.GripMeasureSessionDto createMeasureSession(
            @AuthenticationPrincipal FirebaseUser user,
            @RequestBody GripDtos.CreateGripMeasureSessionRequest req) {
        return useCase.createMeasureSession(user.uid(), req);
    }

    @GetMapping("/me/grip-workouts")
    public List<GripDtos.GripWorkoutDto> myWorkouts(@AuthenticationPrincipal FirebaseUser user) {
        return useCase.myWorkouts(user.uid());
    }

    @GetMapping("/me/grip-workouts/{id}")
    public GripDtos.GripWorkoutDto getWorkout(@AuthenticationPrincipal FirebaseUser user, @PathVariable String id) {
        return useCase.getWorkout(user.uid(), id);
    }

    @PostMapping("/me/grip-workouts")
    @ResponseStatus(HttpStatus.CREATED)
    public GripDtos.GripWorkoutDto createWorkout(@AuthenticationPrincipal FirebaseUser user,
                                                  @RequestBody GripDtos.CreateGripWorkoutRequest req) {
        return useCase.createWorkout(user.uid(), req);
    }

    @PutMapping("/me/grip-workouts/{id}")
    public GripDtos.GripWorkoutDto updateWorkout(@AuthenticationPrincipal FirebaseUser user,
                                                  @PathVariable String id,
                                                  @RequestBody GripDtos.CreateGripWorkoutRequest req) {
        return useCase.updateWorkout(user.uid(), id, req);
    }

    @DeleteMapping("/me/grip-workouts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWorkout(@AuthenticationPrincipal FirebaseUser user, @PathVariable String id) {
        useCase.deleteWorkout(user.uid(), id);
    }
}
