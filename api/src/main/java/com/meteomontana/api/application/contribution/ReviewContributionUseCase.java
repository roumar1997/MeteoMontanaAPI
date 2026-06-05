package com.meteomontana.api.application.contribution;

import com.meteomontana.api.domain.model.PendingContribution;
import com.meteomontana.api.domain.model.SchoolBlock;
import com.meteomontana.api.domain.model.SubmissionStatus;
import com.meteomontana.api.infrastructure.persistence.SpringDataContributionRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SchoolBlockJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataSchoolBlockRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataSchoolRepository;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Aprueba o rechaza una contribución pendiente.
 *
 * Al APROBAR, materializa el resultado según el tipo:
 *   PARKING            → nuevo school_block tipo PARKING
 *   BOULDER            → nuevo school_block tipo BLOCK
 *   SECTOR             → nuevo school_block tipo ZONE
 *   POSITION_CORRECTION → si targetBlockId != null → mueve el bloque;
 *                         si targetBlockId == null  → mueve la escuela entera.
 */
@Service
public class ReviewContributionUseCase {

    private final SpringDataContributionRepository repo;
    private final SpringDataSchoolBlockRepository  blockRepo;
    private final SpringDataSchoolRepository       schoolRepo;

    public ReviewContributionUseCase(SpringDataContributionRepository repo,
                                     SpringDataSchoolBlockRepository blockRepo,
                                     SpringDataSchoolRepository schoolRepo) {
        this.repo       = repo;
        this.blockRepo  = blockRepo;
        this.schoolRepo = schoolRepo;
    }

    @Transactional
    public ContributionResponse approve(String id, FirebaseUser admin) {
        var entity = findPending(id);

        // ── Materializar según tipo ───────────────────────────────────────────
        var c = entity.toDomain();
        switch (c.getType()) {

            case PARKING -> createBlock(c, SchoolBlock.Type.PARKING, admin.uid());
            case BOULDER -> createBlock(c, SchoolBlock.Type.BLOCK,   admin.uid());
            case SECTOR  -> createBlock(c, SchoolBlock.Type.ZONE,    admin.uid());

            case POSITION_CORRECTION -> {
                double newLat = c.getProposedLat() != null ? c.getProposedLat() : c.getLat();
                double newLon = c.getProposedLon() != null ? c.getProposedLon() : c.getLon();

                if (c.getTargetBlockId() != null) {
                    // Mover un bloque existente
                    blockRepo.findById(c.getTargetBlockId()).ifPresent(block -> {
                        block.setLat(newLat);
                        block.setLon(newLon);
                        blockRepo.save(block);
                    });
                } else {
                    // Mover la escuela entera
                    schoolRepo.findById(c.getSchoolId()).ifPresent(school -> {
                        school.setLat(newLat);
                        school.setLon(newLon);
                        schoolRepo.save(school);
                    });
                }
            }
        }

        // ── Marcar como aprobada ──────────────────────────────────────────────
        entity.setStatus(SubmissionStatus.APPROVED);
        entity.setReviewedByUid(admin.uid());
        entity.setReviewReason(null);
        entity.setReviewedAt(LocalDateTime.now());
        repo.save(entity);

        return ContributionResponse.from(entity.toDomain());
    }

    @Transactional
    public ContributionResponse reject(String id, String reason, FirebaseUser admin) {
        var entity = findPending(id);
        entity.setStatus(SubmissionStatus.REJECTED);
        entity.setReviewedByUid(admin.uid());
        entity.setReviewReason(reason);
        entity.setReviewedAt(LocalDateTime.now());
        repo.save(entity);
        return ContributionResponse.from(entity.toDomain());
    }

    // ─────────────────────────────────────────────────────────────────────────

    private com.meteomontana.api.infrastructure.persistence.jpa.PendingContributionJpaEntity
            findPending(String id) {
        var entity = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Contribución no encontrada: " + id));
        if (entity.getStatus() != SubmissionStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta contribución ya fue revisada.");
        }
        return entity;
    }

    private void createBlock(PendingContribution c, SchoolBlock.Type type, String adminUid) {
        var block = new SchoolBlockJpaEntity(
                UUID.randomUUID().toString(),
                c.getSchoolId(),
                type,
                c.getName() != null ? c.getName() : type.name().toLowerCase(),
                c.getLat(),
                c.getLon(),
                c.getPhotoUrl(), // foto de Firebase Storage (null para PARKING/SECTOR)
                c.getNotes(),    // description
                adminUid,
                LocalDateTime.now()
        );
        blockRepo.save(block);
    }
}
