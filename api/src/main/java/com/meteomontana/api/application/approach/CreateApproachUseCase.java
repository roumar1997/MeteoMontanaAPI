package com.meteomontana.api.application.approach;

import com.meteomontana.api.application.admin.AdminGuard;
import com.meteomontana.api.domain.exception.SchoolNotFoundException;
import com.meteomontana.api.domain.model.Approach;
import com.meteomontana.api.domain.port.ApproachRepository;
import com.meteomontana.api.domain.port.SchoolRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alta de una aproximación (camino) — SOLO ADMIN por ahora (consulta legal
 * pendiente para abrirlo a cualquier usuario, ver APPROACH_DESIGN.md §2.6/§10
 * y CLAUDE.md). Nada de cola de revisión: el admin ya es de confianza.
 */
@Service
@RequiredArgsConstructor
public class CreateApproachUseCase {

    public record CreateApproachRequest(
            String fromBlockId, String toBlockId, String name, String pathJson,
            Integer distanceM, Integer ascentM, Integer durationMin,
            String source   // RECORDED / DRAWN / GPX
    ) {}

    private final ApproachRepository approachRepository;
    private final SchoolRepository schoolRepository;
    private final AdminGuard adminGuard;

    @Transactional
    public GetApproachesUseCase.ApproachDto create(String adminUid, String schoolId, CreateApproachRequest req) {
        adminGuard.ensureAdmin(adminUid);
        schoolRepository.findById(schoolId).orElseThrow(() -> new SchoolNotFoundException(schoolId));
        if (req.pathJson() == null || req.pathJson().isBlank()) {
            throw new IllegalArgumentException("pathJson required");
        }
        Approach approach = new Approach(
                UUID.randomUUID().toString(), schoolId, req.fromBlockId(), req.toBlockId(),
                req.name(), req.pathJson(), req.distanceM(), req.ascentM(), req.durationMin(),
                Approach.Source.valueOf(req.source() != null ? req.source() : "DRAWN"),
                // El admin sube el camino ya andado/verificado por él mismo — arranca
                // VERIFIED, no como las contribuciones normales de usuario.
                Approach.Status.VERIFIED,
                adminUid, LocalDateTime.now(), List.of());
        Approach saved = approachRepository.save(approach);
        return toDto(saved);
    }

    private GetApproachesUseCase.ApproachDto toDto(Approach a) {
        return new GetApproachesUseCase.ApproachDto(
                a.getId(), a.getSchoolId(), a.getFromBlockId(), a.getToBlockId(),
                a.getName(), a.getPathJson(), a.getDistanceM(), a.getAscentM(),
                a.getDurationMin(), a.getSource().name(), a.getStatus().name(),
                a.getAuthorUid(), List.of());
    }
}
