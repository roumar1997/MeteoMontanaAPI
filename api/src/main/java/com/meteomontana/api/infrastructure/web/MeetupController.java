package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.meetups.*;
import com.meteomontana.api.domain.port.MeetupRepository;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meetups")
public class MeetupController {

    private final GetMeetupsUseCase getMeetups;
    private final CreateMeetupUseCase createMeetup;
    private final JoinMeetupUseCase joinMeetup;
    private final LeaveMeetupUseCase leaveMeetup;
    private final KickMemberUseCase kickMember;
    private final SubmitReportUseCase submitReport;
    private final GetMeetupAlertUseCase getMeetupAlert;
    private final SetMeetupAlertUseCase setMeetupAlert;
    private final UpdateMeetupUseCase updateMeetup;
    private final MeetupRepository meetupRepository;
    private final MeetupDtoMapper mapper;

    public MeetupController(GetMeetupsUseCase getMeetups,
                            CreateMeetupUseCase createMeetup,
                            JoinMeetupUseCase joinMeetup,
                            LeaveMeetupUseCase leaveMeetup,
                            KickMemberUseCase kickMember,
                            SubmitReportUseCase submitReport,
                            GetMeetupAlertUseCase getMeetupAlert,
                            SetMeetupAlertUseCase setMeetupAlert,
                            UpdateMeetupUseCase updateMeetup,
                            DeleteMeetupUseCase deleteMeetup,
                            MeetupRepository meetupRepository,
                            MeetupDtoMapper mapper) {
        this.getMeetups = getMeetups;
        this.createMeetup = createMeetup;
        this.joinMeetup = joinMeetup;
        this.leaveMeetup = leaveMeetup;
        this.kickMember = kickMember;
        this.submitReport = submitReport;
        this.getMeetupAlert = getMeetupAlert;
        this.setMeetupAlert = setMeetupAlert;
        this.updateMeetup = updateMeetup;
        this.deleteMeetup = deleteMeetup;
        this.meetupRepository = meetupRepository;
        this.mapper = mapper;
    }

    private final DeleteMeetupUseCase deleteMeetup;

    @GetMapping
    public List<MeetupDto> list(
            @AuthenticationPrincipal FirebaseUser user,
            @RequestParam(required = false) String schoolId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String relation) {
        return getMeetups.execute(user.uid(), schoolId, date, relation);
    }

    @GetMapping("/{id}")
    public MeetupDto get(@AuthenticationPrincipal FirebaseUser user, @PathVariable String id) {
        return meetupRepository.findById(id)
                .map(m -> mapper.toDto(m, user.uid()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal FirebaseUser user, @PathVariable String id) {
        deleteMeetup.execute(user.uid(), id);
    }

    /** Abrir el detalle de la quedada desde el chat de grupo (por su conversación). */
    @GetMapping("/by-conversation/{conversationId}")
    public MeetupDto getByConversation(@AuthenticationPrincipal FirebaseUser user,
                                       @PathVariable String conversationId) {
        return meetupRepository.findByConversationId(conversationId)
                .map(m -> mapper.toDto(m, user.uid()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /** Editar la descripción (solo el organizador). */
    @PatchMapping("/{id}")
    public MeetupDto update(@AuthenticationPrincipal FirebaseUser user,
                            @PathVariable String id,
                            @RequestBody UpdateMeetupRequest req) {
        try {
            return updateMeetup.updateDescription(user.uid(), id, req.description());
        } catch (IllegalStateException e) {
            if ("NOT_CREATOR".equals(e.getMessage()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Solo el organizador puede editar la quedada");
            throw e;
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    public record UpdateMeetupRequest(String description) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MeetupDto create(@AuthenticationPrincipal FirebaseUser user,
                            @RequestBody CreateMeetupRequest req) {
        try {
            return createMeetup.execute(user.uid(), req);
        } catch (IllegalStateException e) {
            if ("GENDER_REQUIRED".equals(e.getMessage()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Indica tu género (Mujer) en tu perfil para crear quedadas de solo mujeres");
            throw e;
        }
    }

    @PostMapping("/{id}/join")
    public MeetupDto join(@AuthenticationPrincipal FirebaseUser user, @PathVariable String id) {
        try {
            return joinMeetup.execute(user.uid(), id);
        } catch (IllegalStateException e) {
            throw switch (e.getMessage()) {
                case "FOLLOW_REQUIRED" -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Debes seguir al creador para unirte a esta quedada");
                case "GENDER_REQUIRED" -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Esta quedada es solo para mujeres");
                case "MEETUP_FULL" -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "La quedada está completa");
                default -> new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
            };
        }
    }

    @PostMapping("/{id}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@AuthenticationPrincipal FirebaseUser user, @PathVariable String id) {
        leaveMeetup.execute(user.uid(), id);
    }

    @PostMapping("/{id}/kick")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void kick(@AuthenticationPrincipal FirebaseUser user,
                     @PathVariable String id,
                     @RequestBody Map<String, String> body) {
        String targetUid = body.get("uid");
        if (targetUid == null || targetUid.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "uid requerido");
        kickMember.execute(user.uid(), id, targetUid);
    }

    @PostMapping("/{id}/report")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportDto report(@AuthenticationPrincipal FirebaseUser user,
                            @PathVariable String id,
                            @RequestBody SubmitReportRequest req) {
        return submitReport.execute(user.uid(), id, req);
    }

    // ── Alertas de quedadas ────────────────────────────────────────────────

    @GetMapping("/alerts/me")
    public MeetupAlertDto getMyAlert(@AuthenticationPrincipal FirebaseUser user) {
        return getMeetupAlert.execute(user.uid());
    }

    @PutMapping("/alerts/me")
    public MeetupAlertDto setMyAlert(@AuthenticationPrincipal FirebaseUser user,
                                     @RequestBody SetAlertRequest req) {
        return setMeetupAlert.execute(user.uid(), req.enabled(), req.daysCsv());
    }

    public record SetAlertRequest(boolean enabled, String daysCsv) {}
}
