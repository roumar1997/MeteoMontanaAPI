package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.community.CommunityVoteUseCase;
import com.meteomontana.api.domain.model.CommunityVotes.GradeSummary;
import com.meteomontana.api.domain.model.CommunityVotes.OrientationSummary;
import com.meteomontana.api.domain.model.CommunityVotes.SunHours;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * Votación comunitaria SIN admin: orientación de paredes (cualquiera vota) y
 * grado de vías (solo quien la tiene en el diario). Solo mapea DTO↔dominio;
 * las reglas viven en {@link CommunityVoteUseCase}.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommunityVoteController {

    public record OrientationVoteRequest(Integer photoIndex, @NotBlank String aspect) {}
    public record GradeVoteRequest(@NotBlank String grade) {}

    private final CommunityVoteUseCase useCase;

    @GetMapping("/blocks/{blockId}/orientation")
    public List<OrientationSummary> orientation(@PathVariable String blockId,
                                                @AuthenticationPrincipal FirebaseUser user) {
        return useCase.orientationSummaries(blockId, user.uid());
    }

    @PutMapping("/blocks/{blockId}/orientation")
    public List<OrientationSummary> voteOrientation(@PathVariable String blockId,
                                                    @RequestBody OrientationVoteRequest req,
                                                    @AuthenticationPrincipal FirebaseUser user) {
        return useCase.voteOrientation(blockId, req.photoIndex(), req.aspect(), user.uid());
    }

    /** Consenso de orientación de todas las piedras de una escuela (filtro). */
    @GetMapping("/schools/{schoolId}/orientations")
    public java.util.Map<String, String> schoolOrientations(@PathVariable String schoolId) {
        return useCase.schoolOrientations(schoolId);
    }

    @GetMapping("/blocks/{blockId}/sun-hours")
    public SunHours sunHours(@PathVariable String blockId,
                             @RequestParam(required = false) Integer photoIndex) {
        return useCase.sunHours(blockId, photoIndex, LocalDate.now(ZoneId.of("Europe/Madrid")));
    }

    @GetMapping("/lines/{lineId}/grade-votes")
    public GradeSummary gradeVotes(@PathVariable String lineId,
                                   @AuthenticationPrincipal FirebaseUser user) {
        return useCase.gradeSummary(lineId, user.uid());
    }

    @PutMapping("/lines/{lineId}/grade-votes")
    public GradeSummary voteGrade(@PathVariable String lineId,
                                  @RequestBody GradeVoteRequest req,
                                  @AuthenticationPrincipal FirebaseUser user) {
        return useCase.voteGrade(lineId, req.grade(), user.uid());
    }
}
