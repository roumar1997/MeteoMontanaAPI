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
                // TODO: cuando la app Android esté publicada en Play Store, reactivar el botón
                // "Ver en la app" con un Android App Link tipo https://climbingteams.com/schools/{schoolId}
                // que abra la app si está instalada (requiere assetlinks.json en /.well-known/).
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
        return approve(id, admin, true);
    }

    /** @param notify si false, no manda email de "aprobada" (p.ej. el admin se
     *  auto-aprueba su propia creación → no tiene sentido avisarse a sí mismo). */
    public ContributionResponse approve(String id, FirebaseUser admin, boolean notify) {
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

        if (notify) sendReviewEmail(c, true, null);
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
        // Las PIEDRAS (BLOCK) no llevan nombre libre: se les asigna un NÚMERO
        // secuencial único en la escuela en el momento de materializarse (al
        // aprobar o al crear el admin). Así dos propuestas simultáneas nunca
        // comparten número. PARKING/ZONE sí conservan su nombre propio.
        String name = (type == SchoolBlock.Type.BLOCK)
                ? nextBlockNumber(c.getSchoolId())
                : (c.getName() != null ? c.getName() : type.name().toLowerCase());
        var block = new SchoolBlockJpaEntity(
                UUID.randomUUID().toString(),
                c.getSchoolId(),
                type,
                name,
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

    /** Menor número de piedra LIBRE en la escuela (rellena huecos al borrar:
     *  si existen 1 y 3, devuelve 2; si existen 2 y 3, devuelve 1). Único por escuela. */
    private String nextBlockNumber(String schoolId) {
        java.util.Set<Integer> used = blockRepo.findBySchoolIdOrderByCreatedAtAsc(schoolId).stream()
                .filter(b -> b.getType() == SchoolBlock.Type.BLOCK)
                .map(SchoolBlockJpaEntity::getName)
                .filter(n -> n != null && n.matches("\\d+"))
                .map(Integer::parseInt)
                .collect(java.util.stream.Collectors.toSet());
        int n = 1;
        while (used.contains(n)) n++;
        return String.valueOf(n);
    }

    /**
     * Parsea el JSON `bloquesJson` y añade BlockLineJpaEntity al bloque.
     * Cada vía puede traer su propia `photoUrl` (la CARA sobre la que está
     * dibujada). Las vías se reparten en caras agrupando por foto; cada cara
     * recibe un `faceOrder` según el orden de aparición. Sin `photoUrl` (piedra
     * de una sola foto) caen todas en la foto de portada del bloque (cara 0).
     */
    private void parseAndAttachLines(SchoolBlockJpaEntity block, String bloquesJson) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode arr = mapper.readTree(bloquesJson);
            if (!arr.isArray()) return;

            java.util.LinkedHashMap<String, Integer> faceOrders = new java.util.LinkedHashMap<>();
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

                String facePhoto = facePhotoOf(node, block.getPhotoPath());
                int faceOrder = faceOrders.computeIfAbsent(
                        facePhoto == null ? "" : facePhoto, k -> faceOrders.size());

                BlockLineJpaEntity line = new BlockLineJpaEntity(
                        UUID.randomUUID().toString(),
                        name.isEmpty() ? String.valueOf(sortOrder + 1) : name,
                        grade,
                        startType,
                        linePath,
                        sortOrder++,
                        facePhoto,
                        faceOrder
                );
                block.addLine(line);
            }
        } catch (Exception e) {
            // Si el JSON está mal, simplemente no creamos líneas pero la piedra sí.
        }
    }

    /**
     * La portada del bloque = foto de la CARA 0 (menor faceOrder, luego sortOrder).
     * Tras corregir/añadir vías (que pueden cambiar la foto de una cara), mantiene
     * la portada al día para miniaturas y marcadores del mapa.
     */
    private static void refreshCover(SchoolBlockJpaEntity block) {
        block.getLines().stream()
                .filter(l -> l.getPhotoPath() != null && !l.getPhotoPath().isBlank())
                .min(java.util.Comparator
                        .comparingInt(BlockLineJpaEntity::getFaceOrder)
                        .thenComparingInt(BlockLineJpaEntity::getSortOrder))
                .ifPresent(l -> block.setPhotoPath(l.getPhotoPath()));
    }

    /** Foto (cara) de una vía del JSON: su `photoUrl`, o la portada del bloque. */
    private static String facePhotoOf(JsonNode node, String coverPhoto) {
        String p = node.path("photoUrl").isNull() ? null : node.path("photoUrl").asText(null);
        if (p != null && !p.isBlank()) return p;
        return coverPhoto;
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
            String facePhoto = facePhotoOf(node, block.getPhotoPath());
            if (facePhoto != null && !facePhoto.isBlank()) line.setPhotoPath(facePhoto);
            refreshCover(block);
            blockRepo.save(block);
        } catch (Exception ignored) {}
    }

    /**
     * Materializa una contribución BOULDER con targetBlockId (sin targetLineId
     * a nivel de contribución). Cada entrada de bloquesJson puede llevar un
     * `targetLineId`: si lo trae, CORRIGE esa vía existente; si no, AÑADE una
     * nueva. Así una sola propuesta corrige varias vías y/o añade nuevas
     * (editor unificado de iOS). Retrocompatible: las propuestas antiguas de
     * solo-añadir no traen targetLineId y caen en el "añadir".
     */
    private void addLinesToExistingBlock(PendingContribution c) {
        var blockOpt = blockRepo.findById(c.getTargetBlockId());
        if (blockOpt.isEmpty()) return;
        var block = blockOpt.get();
        if (c.getBloquesJson() == null || c.getBloquesJson().isBlank()) return;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode arr = mapper.readTree(c.getBloquesJson());
            if (!arr.isArray()) return;
            int sortOrder = block.getLines().stream()
                    .mapToInt(BlockLineJpaEntity::getSortOrder).max().orElse(-1) + 1;
            // Mapa foto→orden de cara con las caras ya existentes (para que las vías
            // nuevas de una foto ya conocida caigan en su cara, y las de una foto
            // nueva creen una cara nueva al final).
            java.util.LinkedHashMap<String, Integer> faceOrders = new java.util.LinkedHashMap<>();
            for (var l : block.getLines()) {
                String key = l.getPhotoPath() != null ? l.getPhotoPath()
                        : (block.getPhotoPath() != null ? block.getPhotoPath() : "");
                faceOrders.putIfAbsent(key, l.getFaceOrder());
            }
            int[] nextFaceOrder = { faceOrders.values().stream()
                    .mapToInt(Integer::intValue).max().orElse(-1) + 1 };
            for (JsonNode node : arr) {
                final String name = node.path("name").asText("").trim();
                String g = node.path("grade").isNull() ? null : node.path("grade").asText("").trim();
                final String grade = (g != null && g.isEmpty()) ? null : g;
                String rawStart = node.path("startType").isNull() ? null : node.path("startType").asText("").trim();
                final BlockLine.StartType startType = mapStartType(rawStart);
                final String linePath = node.path("linePath").asText(null);
                final String facePhoto = facePhotoOf(node, block.getPhotoPath());
                String tId = node.path("targetLineId").isNull() ? null : node.path("targetLineId").asText(null);

                if (tId != null && !tId.isBlank()) {
                    // Corrige una vía existente de este mismo bloque.
                    block.getLines().stream()
                            .filter(l -> l.getId().equals(tId)).findFirst()
                            .ifPresent(line -> {
                                if (!name.isEmpty()) line.setName(name);
                                line.setGrade(grade);
                                line.setStartType(startType);
                                if (linePath != null && !linePath.isBlank()) line.setLinePath(linePath);
                                if (facePhoto != null && !facePhoto.isBlank()) line.setPhotoPath(facePhoto);
                            });
                } else {
                    String key = facePhoto == null ? "" : facePhoto;
                    Integer fo = faceOrders.get(key);
                    if (fo == null) { fo = nextFaceOrder[0]++; faceOrders.put(key, fo); }
                    block.addLine(new BlockLineJpaEntity(
                            UUID.randomUUID().toString(),
                            name.isEmpty() ? String.valueOf(sortOrder + 1) : name,
                            grade, startType, linePath, sortOrder++,
                            facePhoto, fo));
                }
            }
            refreshCover(block);
            blockRepo.save(block);
        } catch (Exception ignored) {}
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
