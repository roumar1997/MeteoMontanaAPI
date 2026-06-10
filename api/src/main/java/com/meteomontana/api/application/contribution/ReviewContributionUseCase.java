package com.meteomontana.api.application.contribution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meteomontana.api.domain.model.BlockLine;
import com.meteomontana.api.domain.model.PendingContribution;
import com.meteomontana.api.domain.model.SchoolBlock;
import com.meteomontana.api.domain.model.SubmissionStatus;
import com.meteomontana.api.infrastructure.persistence.SpringDataContributionRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.BlockLineJpaEntity;
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
    private final com.meteomontana.api.infrastructure.email.ResendEmailService emailService;
    private final com.meteomontana.api.domain.port.UserRepository userRepository;

    public ReviewContributionUseCase(SpringDataContributionRepository repo,
                                     SpringDataSchoolBlockRepository blockRepo,
                                     SpringDataSchoolRepository schoolRepo,
                                     com.meteomontana.api.infrastructure.email.ResendEmailService emailService,
                                     com.meteomontana.api.domain.port.UserRepository userRepository) {
        this.repo       = repo;
        this.blockRepo  = blockRepo;
        this.schoolRepo = schoolRepo;
        this.emailService = emailService;
        this.userRepository = userRepository;
    }

    private void sendReviewEmail(PendingContribution c, boolean approved, String reason) {
        var user = userRepository.findByUid(c.getSubmittedByUid()).orElse(null);
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) return;
        String typeLabel = switch (c.getType()) {
            case PARKING -> "parking"; case BOULDER -> "piedra";
            case SECTOR -> "sector"; case POSITION_CORRECTION -> "corrección de posición";
            case ASSIGN_SECTOR -> "asignación de sector";
        };

        // "parking de El Escorial · Parking principal"
        String proposalLabel = typeLabel + " en " + c.getSchoolName()
                + (c.getName() != null && !c.getName().isBlank() ? " · " + c.getName() : "");

        String subject;
        String inner;
        if (approved) {
            subject = "✅ Tu propuesta ya está publicada en Cumbre";
            inner = com.meteomontana.api.infrastructure.email.EmailTemplates.eyebrow("Propuesta aprobada")
                + com.meteomontana.api.infrastructure.email.EmailTemplates.title("¡Ya está en el mapa!")
                + com.meteomontana.api.infrastructure.email.EmailTemplates.paragraph(
                        "Hemos revisado tu propuesta y la hemos publicado. "
                        + "Desde ahora cualquier escalador puede verla en la app.")
                + com.meteomontana.api.infrastructure.email.EmailTemplates.highlightBox(
                        "Tu propuesta",
                        com.meteomontana.api.infrastructure.email.EmailTemplates.escape(proposalLabel))
                + com.meteomontana.api.infrastructure.email.EmailTemplates.paragraph(
                        "Gracias por hacer crecer la guía entre todos. "
                        + "La comunidad escaladora te lo agradece. 🤘")
                + com.meteomontana.api.infrastructure.email.EmailTemplates.button(
                        "Ver en la app", "https://climbingteams.com")
                + com.meteomontana.api.infrastructure.email.EmailTemplates.signature();
        } else {
            subject = "Tu propuesta en Cumbre no se ha podido publicar";
            inner = com.meteomontana.api.infrastructure.email.EmailTemplates.eyebrow("Propuesta revisada")
                + com.meteomontana.api.infrastructure.email.EmailTemplates.title("Esta vez no ha podido ser")
                + com.meteomontana.api.infrastructure.email.EmailTemplates.paragraph(
                        "Hemos revisado tu propuesta y no la hemos podido publicar.")
                + com.meteomontana.api.infrastructure.email.EmailTemplates.highlightBox(
                        "Tu propuesta",
                        com.meteomontana.api.infrastructure.email.EmailTemplates.escape(proposalLabel))
                + (reason != null && !reason.isBlank()
                        ? com.meteomontana.api.infrastructure.email.EmailTemplates.highlightBox(
                                "Motivo",
                                com.meteomontana.api.infrastructure.email.EmailTemplates.escape(reason))
                        : "")
                + com.meteomontana.api.infrastructure.email.EmailTemplates.paragraph(
                        "Si crees que es un error o quieres dar más detalles, "
                        + "puedes volver a enviarla o escribirnos desde la app.")
                + com.meteomontana.api.infrastructure.email.EmailTemplates.signature();
        }

        String preheader = approved
                ? "Tu " + proposalLabel + " ya está publicada."
                : "Hemos revisado tu " + proposalLabel + ".";
        emailService.send(user.getEmail(), subject,
                com.meteomontana.api.infrastructure.email.EmailTemplates.wrap(preheader, inner));
    }

    @Transactional
    public ContributionResponse approve(String id, FirebaseUser admin) {
        var entity = findPending(id);

        // ── Materializar según tipo ───────────────────────────────────────────
        var c = entity.toDomain();
        switch (c.getType()) {

            case PARKING -> createBlock(c, SchoolBlock.Type.PARKING, admin.uid());
            case BOULDER -> {
                if (c.getTargetBlockId() != null && c.getTargetLineId() != null) {
                    // Corrección de una vía concreta: actualiza la línea existente
                    // con los datos del primer bloque del JSON.
                    updateExistingLine(c);
                } else if (c.getTargetBlockId() != null) {
                    // Añadir vías al bloque existente.
                    addLinesToExistingBlock(c);
                } else {
                    createBlock(c, SchoolBlock.Type.BLOCK, admin.uid());
                }
            }
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

        sendReviewEmail(c, true, null);
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
        sendReviewEmail(entity.toDomain(), false, reason);
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
                LocalDateTime.now(),
                type == SchoolBlock.Type.BLOCK ? c.getSectorBlockId() : null
        );

        // Para BOULDER: parsear bloquesJson y crear las líneas (vías) del bloque.
        // Cascade ALL del @OneToMany hace que se persistan al guardar el SchoolBlockJpaEntity.
        if (type == SchoolBlock.Type.BLOCK && c.getBloquesJson() != null
                && !c.getBloquesJson().isBlank()) {
            parseAndAttachLines(block, c.getBloquesJson());
        }

        blockRepo.save(block);
    }

    /** Parsea el JSON `bloquesJson` y añade BlockLineJpaEntity al bloque. */
    private void parseAndAttachLines(SchoolBlockJpaEntity block, String bloquesJson) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode arr = mapper.readTree(bloquesJson);
            if (!arr.isArray()) return;

            int sortOrder = 0;
            for (JsonNode node : arr) {
                String name = node.path("name").asText("").trim();
                String grade = node.path("grade").isNull() ? null
                        : node.path("grade").asText("").trim();
                if (grade != null && grade.isEmpty()) grade = null;

                // App envía PIE/SIT/LANCE/TRAV; BD acepta STAND/SIT/JUMP/TRAV.
                String rawStart = node.path("startType").isNull() ? null
                        : node.path("startType").asText("").trim();
                BlockLine.StartType startType = mapStartType(rawStart);

                String linePath = node.path("linePath").asText(null);

                BlockLineJpaEntity line = new BlockLineJpaEntity(
                        UUID.randomUUID().toString(),
                        name.isEmpty() ? String.valueOf(sortOrder + 1) : name,
                        grade,
                        startType,
                        linePath,
                        sortOrder++
                );
                block.addLine(line);
            }
        } catch (Exception e) {
            // Si el JSON está mal, simplemente no creamos líneas pero la piedra sí.
        }
    }

    /**
     * Actualiza una línea existente con los datos del primer bloque del JSON.
     * Usado para correcciones de vía (targetBlockId + targetLineId != null).
     */
    private void updateExistingLine(PendingContribution c) {
        var blockOpt = blockRepo.findById(c.getTargetBlockId());
        if (blockOpt.isEmpty()) return;
        var block = blockOpt.get();
        var lineOpt = block.getLines().stream()
                .filter(l -> l.getId().equals(c.getTargetLineId()))
                .findFirst();
        if (lineOpt.isEmpty()) return;
        var line = lineOpt.get();

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode arr = mapper.readTree(c.getBloquesJson());
            if (!arr.isArray() || arr.size() == 0) return;
            JsonNode node = arr.get(0);

            String name = node.path("name").asText("").trim();
            String grade = node.path("grade").isNull() ? null
                    : node.path("grade").asText("").trim();
            if (grade != null && grade.isEmpty()) grade = null;
            String rawStart = node.path("startType").isNull() ? null
                    : node.path("startType").asText("").trim();
            BlockLine.StartType startType = mapStartType(rawStart);
            String linePath = node.path("linePath").asText(null);

            if (!name.isEmpty()) line.setName(name);
            line.setGrade(grade);
            line.setStartType(startType);
            if (linePath != null && !linePath.isBlank()) line.setLinePath(linePath);
            blockRepo.save(block);
        } catch (Exception ignored) {}
    }

    /** Añade líneas (vías) al bloque existente identificado por targetBlockId. */
    private void addLinesToExistingBlock(PendingContribution c) {
        var blockOpt = blockRepo.findById(c.getTargetBlockId());
        if (blockOpt.isEmpty()) return;
        var block = blockOpt.get();
        // Calcular el siguiente sortOrder a partir de las líneas existentes
        int baseSort = block.getLines().stream()
                .mapToInt(BlockLineJpaEntity::getSortOrder).max().orElse(-1) + 1;
        parseAndAttachLinesWithOffset(block, c.getBloquesJson(), baseSort);
        blockRepo.save(block);
    }

    /** Variante que respeta un offset inicial para sortOrder. */
    private void parseAndAttachLinesWithOffset(SchoolBlockJpaEntity block,
                                                String bloquesJson, int baseSort) {
        if (bloquesJson == null || bloquesJson.isBlank()) return;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode arr = mapper.readTree(bloquesJson);
            if (!arr.isArray()) return;
            int sortOrder = baseSort;
            for (JsonNode node : arr) {
                String name = node.path("name").asText("").trim();
                String grade = node.path("grade").isNull() ? null
                        : node.path("grade").asText("").trim();
                if (grade != null && grade.isEmpty()) grade = null;
                String rawStart = node.path("startType").isNull() ? null
                        : node.path("startType").asText("").trim();
                BlockLine.StartType startType = mapStartType(rawStart);
                String linePath = node.path("linePath").asText(null);
                BlockLineJpaEntity line = new BlockLineJpaEntity(
                        UUID.randomUUID().toString(),
                        name.isEmpty() ? String.valueOf(sortOrder + 1) : name,
                        grade, startType, linePath, sortOrder++
                );
                block.addLine(line);
            }
        } catch (Exception ignored) {}
    }

    private static BlockLine.StartType mapStartType(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return switch (raw.toUpperCase()) {
            case "PIE", "STAND" -> BlockLine.StartType.STAND;
            case "SIT"          -> BlockLine.StartType.SIT;
            case "LANCE", "JUMP" -> BlockLine.StartType.JUMP;
            case "TRAV"         -> BlockLine.StartType.TRAV;
            default             -> null;
        };
    }
}
