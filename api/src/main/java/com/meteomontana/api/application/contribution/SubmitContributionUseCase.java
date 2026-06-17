package com.meteomontana.api.application.contribution;

import com.meteomontana.api.domain.exception.SchoolNotFoundException;
import com.meteomontana.api.domain.model.PendingContribution;
import com.meteomontana.api.domain.model.SubmissionStatus;
import com.meteomontana.api.domain.port.SchoolRepository;
import com.meteomontana.api.infrastructure.persistence.SpringDataContributionRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.PendingContributionJpaEntity;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SubmitContributionUseCase {

    private final SpringDataContributionRepository repo;
    private final SchoolRepository schoolRepository;
    private final com.meteomontana.api.application.admin.AdminGuard adminGuard;
    private final ReviewContributionUseCase reviewUseCase;

    public SubmitContributionUseCase(SpringDataContributionRepository repo,
                                     SchoolRepository schoolRepository,
                                     com.meteomontana.api.application.admin.AdminGuard adminGuard,
                                     ReviewContributionUseCase reviewUseCase) {
        this.repo = repo;
        this.schoolRepository = schoolRepository;
        this.adminGuard = adminGuard;
        this.reviewUseCase = reviewUseCase;
    }

    public ContributionResponse execute(String schoolId, ContributionRequest req,
                                        FirebaseUser user) {
        var school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new SchoolNotFoundException(schoolId));

        PendingContribution.Type type;
        try {
            type = PendingContribution.Type.valueOf(req.type().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tipo de contribución inválido: " + req.type());
        }

        var contribution = new PendingContribution(
                UUID.randomUUID().toString(), type, SubmissionStatus.PENDING,
                school.getId(), school.getName(),
                req.name(), req.lat(), req.lon(),
                req.notes(), req.description(),
                req.proposedLat(), req.proposedLon(), req.correctionReason(),
                req.targetBlockId(), req.targetLineId(), req.sectorBlockId(),
                req.photoUrl(), req.bloquesJson(), req.topoLinesJson(),
                user.uid(), user.name(),
                null, null,
                LocalDateTime.now(), null
        );

        repo.save(PendingContributionJpaEntity.from(contribution));

        // Si quien propone es ADMIN, se publica directamente (sin cola de
        // revisión): materializamos al instante reutilizando la aprobación.
        // Cubre crear piedra/parking/sector y corregir posición (incl. la
        // escuela) en ambas apps, sin cambios de UI.
        if (adminGuard.isAdmin(user.uid())) {
            // notify=false: no avisar por email al propio admin de su creación.
            return reviewUseCase.approve(contribution.getId(), user, false);
        }
        return ContributionResponse.from(contribution);
    }
}
