package com.meteomontana.api.application.contribution;

import com.meteomontana.api.application.feed.FeedPublishService;
import com.meteomontana.api.application.feed.FeedViews;
import com.meteomontana.api.domain.exception.ConflictException;
import com.meteomontana.api.domain.exception.NotFoundException;
import com.meteomontana.api.domain.model.PendingContribution;
import com.meteomontana.api.domain.model.SchoolBlock;
import com.meteomontana.api.domain.model.SubmissionStatus;
import com.meteomontana.api.infrastructure.persistence.SpringDataContributionRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.BlockLineJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.PendingContributionJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SchoolBlockJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataSchoolBlockRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataSchoolRepository;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * ORQUESTA la aprobación/rechazo de una contribución pendiente y delega el
 * trabajo (SRP — antes esta clase hacía las 4 cosas ella misma, 725 líneas):
 *   - [BlockMaterializer]  → crear school_blocks nuevos (PARKING/BOULDER/SECTOR)
 *   - [LineReconciler]     → aplicar cambios sobre bloques existentes
 *   - [ReviewNotifier]     → email al autor (best-effort)
 *   - [FeedPublishService] → posts automáticos NEW_BLOCK/NEW_LINE
 *
 * Al APROBAR, materializa el resultado según el tipo:
 *   PARKING            → nuevo school_block tipo PARKING
 *   BOULDER            → nuevo school_block tipo BLOCK (o edición del existente)
 *   SECTOR             → nuevo school_block tipo ZONE
 *   POSITION_CORRECTION → si targetBlockId != null → mueve el bloque;
 *                         si targetBlockId == null  → mueve la escuela entera.
 */
@Service
public class ReviewContributionUseCase {

    private static final org.slf4j.Logger log =
        org.slf4j.LoggerFactory.getLogger(ReviewContributionUseCase.class);

    private final SpringDataContributionRepository repo;
    private final SpringDataSchoolBlockRepository  blockRepo;
    private final SpringDataSchoolRepository       schoolRepo;
    private final BlockMaterializer materializer;
    private final LineReconciler reconciler;
    private final ReviewNotifier notifier;
    private final FeedPublishService feedService;

    public ReviewContributionUseCase(SpringDataContributionRepository repo,
                                     SpringDataSchoolBlockRepository blockRepo,
                                     SpringDataSchoolRepository schoolRepo,
                                     BlockMaterializer materializer,
                                     LineReconciler reconciler,
                                     ReviewNotifier notifier,
                                     FeedPublishService feedService) {
        this.repo       = repo;
        this.blockRepo  = blockRepo;
        this.schoolRepo = schoolRepo;
        this.materializer = materializer;
        this.reconciler = reconciler;
        this.notifier = notifier;
        this.feedService = feedService;
    }

    /**
     * Post automático del feed al aprobar (NEW_BLOCK para piedra nueva, NEW_LINE
     * si se añadieron vías a una piedra ya existente), con autor = autor de la
     * contribución. Sin push. Un fallo aquí NUNCA tumba la aprobación.
     */
    private void publishFeedPost(PendingContribution c, SchoolBlockJpaEntity block,
                                 BlockLineJpaEntity firstNewLine, String kind) {
        try {
            if (block == null || c.getSubmittedByUid() == null) return;
            feedService.publishSystem(c.getSubmittedByUid(), block, firstNewLine, kind);
        } catch (Exception e) {
            log.warn("Post de feed ({}) de la contribución {} FALLÓ: {}",
                    kind, c.getId(), e.toString());
        }
    }

    @Transactional
    public ContributionResponse approve(String id, FirebaseUser admin) {
        return approve(id, admin, true);
    }

    public ContributionResponse approve(String id, FirebaseUser admin, boolean notify) {
        return approve(id, admin, notify, null);
    }

    /** @param notify si false, no manda email de "aprobada" (p.ej. el admin se
     *  auto-aprueba su propia creación → no tiene sentido avisarse a sí mismo).
     *  @param editedBloquesJson si != null, "EDITAR Y APROBAR": el admin retocó
     *  la propuesta y se materializa CON sus cambios (se persiste en la
     *  contribución para que la auditoría refleje lo realmente aplicado). */
    // OJO: SIN @Transactional a propósito (comportamiento probado en prod). El
    // materializar hace varios repo.save() y cada uno se auto-confirma por su
    // cuenta. Envolverlo en UNA transacción (intentado el 2026-07-19) rompía la
    // creación de PIEDRAS NUEVAS con vías: bajo la transacción envolvente, el
    // save() de una piedra con hijos (cascade) omitía el INSERT del padre en
    // silencio → la propuesta quedaba APPROVED pero la piedra no se creaba
    // (cazado por Rodrigo en staging, 2026-07-20). No re-envolver sin un test de
    // integración con Postgres que lo cubra.
    public ContributionResponse approve(String id, FirebaseUser admin, boolean notify,
                                        String editedBloquesJson) {
        var entity = findPending(id);
        if (editedBloquesJson != null && !editedBloquesJson.isBlank()) {
            entity.setBloquesJson(editedBloquesJson);
        }

        // ── Materializar según tipo ───────────────────────────────────────────
        var c = entity.toDomain();
        switch (c.getType()) {

            case PARKING -> materializer.createBlock(c, SchoolBlock.Type.PARKING, admin.uid());
            case BOULDER -> {
                if (c.getTargetBlockId() != null && c.getGeometry() != null) {
                    // Edición de MURO: el payload es el estado COMPLETO propuesto
                    // (vías con su lineId + path + dirección) → reconciliar
                    // preservando ids (los enganches del diario sobreviven).
                    // Si la edición CREÓ vías nuevas → un post NEW_LINE (uno por
                    // contribución, referenciando la primera vía nueva).
                    BlockLineJpaEntity firstNew = reconciler.reconcileWall(c);
                    if (firstNew != null) {
                        publishFeedPost(c, blockRepo.findById(c.getTargetBlockId()).orElse(null),
                                firstNew, FeedViews.KIND_NEW_LINE);
                    }
                } else if (c.getTargetBlockId() != null && c.getTargetLineId() != null) {
                    // Corrección de una vía concreta: actualiza la línea existente
                    // con los datos del primer bloque del JSON. Sin post de feed.
                    reconciler.updateExistingLine(c);
                } else if (c.getTargetBlockId() != null) {
                    // Añadir vías al bloque existente → NEW_LINE si creó alguna.
                    BlockLineJpaEntity firstNew = reconciler.addLinesToExistingBlock(c);
                    if (firstNew != null) {
                        publishFeedPost(c, blockRepo.findById(c.getTargetBlockId()).orElse(null),
                                firstNew, FeedViews.KIND_NEW_LINE);
                    }
                } else {
                    // Piedra NUEVA → un post NEW_BLOCK (por piedra, no por vía).
                    SchoolBlockJpaEntity created = materializer.createBlock(c, SchoolBlock.Type.BLOCK, admin.uid());
                    publishFeedPost(c, created, null, FeedViews.KIND_NEW_BLOCK);
                }
            }
            case SECTOR  -> materializer.createBlock(c, SchoolBlock.Type.ZONE, admin.uid());

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

            case ASSIGN_SECTOR -> {
                if (c.getTargetBlockId() != null && c.getSectorBlockId() != null) {
                    blockRepo.findById(c.getTargetBlockId()).ifPresent(block -> {
                        block.setSectorBlockId(c.getSectorBlockId());
                        blockRepo.save(block);
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

        if (notify) notifier.sendReviewEmail(c, true, null);
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
        notifier.sendReviewEmail(entity.toDomain(), false, reason);
        return ContributionResponse.from(entity.toDomain());
    }

    // ─────────────────────────────────────────────────────────────────────────

    private PendingContributionJpaEntity findPending(String id) {
        var entity = repo.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Contribución no encontrada: " + id));
        if (entity.getStatus() != SubmissionStatus.PENDING) {
            throw new ConflictException(
                    "Esta contribución ya fue revisada.");
        }
        return entity;
    }
}
