package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.contribution.*;
import com.meteomontana.api.domain.model.SubmissionStatus;
import com.meteomontana.api.domain.port.PendingContributionRepository;
import com.meteomontana.api.application.admin.AdminGuard;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * Endpoints de propuestas de mejora de escuelas existentes.
 *
 *  POST   /api/schools/{id}/contributions          → usuario autenticado envía propuesta
 *  GET    /api/contributions/me                    → mis propuestas
 *  GET    /api/admin/contributions                 → cola pending (admin)
 *  POST   /api/admin/contributions/{id}/approve    → aprobar (admin)
 *  POST   /api/admin/contributions/{id}/reject     → rechazar con motivo (admin)
 */
@RestController
@RequiredArgsConstructor
public class ContributionController {

    private final SubmitContributionUseCase submitUseCase;
    private final ReviewContributionUseCase reviewUseCase;
    private final PendingContributionRepository repo;
    private final AdminGuard adminGuard;

    /** Usuario envía propuesta para una escuela concreta. */
    @PostMapping("/api/schools/{schoolId}/contributions")
    @ResponseStatus(HttpStatus.CREATED)
    public ContributionResponse submit(
            @PathVariable String schoolId,
            @Valid @RequestBody ContributionRequest req,
            @AuthenticationPrincipal FirebaseUser user) {
        return submitUseCase.execute(schoolId, req, user);
    }

    /** Mis propuestas (todas las escuelas). */
    @GetMapping("/api/contributions/me")
    public List<ContributionResponse> myContributions(
            @AuthenticationPrincipal FirebaseUser user) {
        return repo.findBySubmitter(user.uid())
                .stream().map(ContributionResponse::from).toList();
    }

    /** Cola pendiente para admin. */
    @GetMapping("/api/admin/contributions")
    public List<ContributionResponse> adminQueue(
            @AuthenticationPrincipal FirebaseUser user,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String status) {
        adminGuard.ensureAdmin(user.uid());
        // P6: sin status = PENDING (compat); APPROVED/REJECTED = historial.
        var list = status == null ? repo.findPending()
                : repo.findByStatus(SubmissionStatus.valueOf(status.toUpperCase()));
        return list.stream().map(ContributionResponse::from).toList();
    }

    /** Admin aprueba. Body opcional {"bloquesJson": "..."} = "EDITAR Y
     *  APROBAR": el admin retoca la propuesta (líneas, nombres, grados...) y
     *  se aprueba CON sus cambios; el autor sigue siendo el proponente. */
    @PostMapping("/api/admin/contributions/{id}/approve")
    public ContributionResponse approve(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal FirebaseUser user) {
        adminGuard.ensureAdmin(user.uid());
        String edited = body != null ? body.get("bloquesJson") : null;
        return reviewUseCase.approve(id, user, true, edited);
    }

    /** Admin rechaza (con motivo opcional en el body). */
    @PostMapping("/api/admin/contributions/{id}/reject")
    public ContributionResponse reject(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal FirebaseUser user) {
        adminGuard.ensureAdmin(user.uid());
        String reason = body != null ? body.get("reason") : null;
        return reviewUseCase.reject(id, reason, user);
    }
}
