package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.admin.AdminGuard;
import com.meteomontana.api.application.blocks.RateLineUseCase;
import com.meteomontana.api.application.blocks.SchoolBlockUseCase;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SchoolBlockController {

    private final SchoolBlockUseCase useCase;
    private final AdminGuard adminGuard;
    private final RateLineUseCase rateLine;

    /** Público: listar bloques de la escuela. Si el usuario está autenticado,
     *  las vías incluyen su valoración personal (myStars). */
    @GetMapping("/schools/{id}/blocks")
    public List<SchoolBlockUseCase.BlockDto> list(
            @PathVariable String id,
            @AuthenticationPrincipal FirebaseUser user) {
        return useCase.listBySchool(id, user != null ? user.uid() : null);
    }

    /** Público: detalle de un bloque (con sus líneas). */
    @GetMapping("/blocks/{id}")
    public SchoolBlockUseCase.BlockDto get(@PathVariable String id) {
        return useCase.findById(id);
    }

    /** Crear bloque directamente: SOLO admin. Los usuarios normales proponen
     *  bloques vía /api/schools/{id}/contributions (cola de revisión), no aquí.
     *  Sin esta restricción cualquier usuario autenticado podía insertar bloques
     *  en producción sin revisión (spam). */
    @PostMapping("/schools/{id}/blocks")
    @ResponseStatus(HttpStatus.CREATED)
    public SchoolBlockUseCase.BlockDto create(
            @AuthenticationPrincipal FirebaseUser user,
            @PathVariable String id,
            @RequestBody SchoolBlockUseCase.CreateBlockRequest req) {
        adminGuard.ensureAdmin(user.uid());
        return useCase.create(user.uid(), id, req);
    }

    @PutMapping("/blocks/{blockId}")
    public SchoolBlockUseCase.BlockDto update(
            @AuthenticationPrincipal FirebaseUser user,
            @PathVariable String blockId,
            @RequestBody SchoolBlockUseCase.CreateBlockRequest req) {
        return useCase.update(user.uid(), blockId, req);
    }

    @DeleteMapping("/blocks/{blockId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal FirebaseUser user,
                       @PathVariable String blockId) {
        useCase.delete(user.uid(), blockId);
    }

    // ── Valoraciones de vías ────────────────────────────────────────────────

    public record RateRequest(int stars) {}

    /** Valora una vía (1-5 estrellas). Upsert: si ya valoró, actualiza. */
    @PostMapping("/blocks/{blockId}/lines/{lineId}/rate")
    public RateLineUseCase.RatingResult rate(
            @AuthenticationPrincipal FirebaseUser user,
            @PathVariable String blockId,
            @PathVariable String lineId,
            @RequestBody RateRequest req) {
        return rateLine.rate(user.uid(), lineId, req.stars());
    }

    /** Elimina la valoración del usuario para esta vía. */
    @DeleteMapping("/blocks/{blockId}/lines/{lineId}/rate")
    public RateLineUseCase.RatingResult unrate(
            @AuthenticationPrincipal FirebaseUser user,
            @PathVariable String blockId,
            @PathVariable String lineId) {
        return rateLine.unrate(user.uid(), lineId);
    }
}
