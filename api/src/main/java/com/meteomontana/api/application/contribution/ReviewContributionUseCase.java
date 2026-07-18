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

    private static final org.slf4j.Logger log =
        org.slf4j.LoggerFactory.getLogger(ReviewContributionUseCase.class);

    private final SpringDataContributionRepository repo;
    private final SpringDataSchoolBlockRepository  blockRepo;
    private final SpringDataSchoolRepository       schoolRepo;
    private final com.meteomontana.api.infrastructure.email.ResendEmailService emailService;
    private final com.meteomontana.api.domain.port.UserRepository userRepository;
    private final com.meteomontana.api.infrastructure.persistence.jpa.SpringDataJournalRepository journalRepo;
    private final com.meteomontana.api.application.feed.FeedService feedService;

    public ReviewContributionUseCase(SpringDataContributionRepository repo,
                                     SpringDataSchoolBlockRepository blockRepo,
                                     SpringDataSchoolRepository schoolRepo,
                                     com.meteomontana.api.infrastructure.email.ResendEmailService emailService,
                                     com.meteomontana.api.domain.port.UserRepository userRepository,
                                     com.meteomontana.api.infrastructure.persistence.jpa.SpringDataJournalRepository journalRepo,
                                     com.meteomontana.api.application.feed.FeedService feedService) {
        this.repo       = repo;
        this.blockRepo  = blockRepo;
        this.schoolRepo = schoolRepo;
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.journalRepo = journalRepo;
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

    /** Propaga el grado nuevo de una vía al diario de todos (si la vía tiene id). */
    private void propagateGrade(String lineId, String grade) {
        if (lineId != null && !lineId.isBlank()) journalRepo.updateGradeByLineId(lineId, grade);
    }

    private void sendReviewEmail(PendingContribution c, boolean approved, String reason) {
        // Un fallo construyendo/enviando el email NUNCA debe tumbar la revisión
        // (esto corre dentro de la transacción del approve/reject).
        try {
            doSendReviewEmail(c, approved, reason);
        } catch (Exception e) {
            log.warn("Email de revisión (approved={}) de {} FALLÓ: {}",
                approved, c.getId(), e.toString());
        }
    }

    private void doSendReviewEmail(PendingContribution c, boolean approved, String reason) {
        var user = userRepository.findByUid(c.getSubmittedByUid()).orElse(null);
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            log.info("Email de revisión (approved={}) de {} OMITIDO: autor {} sin email en BD",
                approved, c.getId(), c.getSubmittedByUid());
            return;
        }
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
        boolean sent = emailService.send(user.getEmail(), subject,
                com.meteomontana.api.infrastructure.email.EmailTemplates.wrap(preheader, inner));
        // Rastro para diagnosticar "el email no llega": buscar "Email de revisión"
        // en los logs de Railway dice si se intentó, a quién y si Resend lo aceptó.
        log.info("Email de revisión (approved={}) de {} a {}: enviado={}",
                approved, c.getId(), user.getEmail(), sent);
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
                if (c.getTargetBlockId() != null && c.getGeometry() != null) {
                    // Edición de MURO: el payload es el estado COMPLETO propuesto
                    // (vías con su lineId + path + dirección) → reconciliar
                    // preservando ids (los enganches del diario sobreviven).
                    // Si la edición CREÓ vías nuevas → un post NEW_LINE (uno por
                    // contribución, referenciando la primera vía nueva).
                    BlockLineJpaEntity firstNew = reconcileWall(c);
                    if (firstNew != null) {
                        publishFeedPost(c, blockRepo.findById(c.getTargetBlockId()).orElse(null),
                                firstNew, com.meteomontana.api.application.feed.FeedService.KIND_NEW_LINE);
                    }
                } else if (c.getTargetBlockId() != null && c.getTargetLineId() != null) {
                    // Corrección de una vía concreta: actualiza la línea existente
                    // con los datos del primer bloque del JSON. Sin post de feed.
                    updateExistingLine(c);
                } else if (c.getTargetBlockId() != null) {
                    // Añadir vías al bloque existente → NEW_LINE si creó alguna.
                    BlockLineJpaEntity firstNew = addLinesToExistingBlock(c);
                    if (firstNew != null) {
                        publishFeedPost(c, blockRepo.findById(c.getTargetBlockId()).orElse(null),
                                firstNew, com.meteomontana.api.application.feed.FeedService.KIND_NEW_LINE);
                    }
                } else {
                    // Piedra NUEVA → un post NEW_BLOCK (por piedra, no por vía).
                    SchoolBlockJpaEntity created = createBlock(c, SchoolBlock.Type.BLOCK, admin.uid());
                    publishFeedPost(c, created, null,
                            com.meteomontana.api.application.feed.FeedService.KIND_NEW_BLOCK);
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

    private SchoolBlockJpaEntity createBlock(PendingContribution c, SchoolBlock.Type type, String adminUid) {
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
        // Modalidad de la piedra (bloque/vía) elegida por el autor de la propuesta.
        if (type == SchoolBlock.Type.BLOCK) {
            block.setDiscipline(parseDiscipline(c.getDiscipline()));
            // Geometría: punto o muro (polilínea).
            SchoolBlock.Geometry geom = parseGeometry(c.getGeometry());
            block.setGeometry(geom);
            block.setPath(geom == SchoolBlock.Geometry.LINE ? c.getPath() : null);
            block.setDirection("RTL".equalsIgnoreCase(c.getDirection()) ? "RTL" : "LTR");
        }

        // Para BOULDER: parsear bloquesJson y crear las líneas (vías) del bloque.
        // Cascade ALL del @OneToMany hace que se persistan al guardar el SchoolBlockJpaEntity.
        if (type == SchoolBlock.Type.BLOCK && c.getBloquesJson() != null
                && !c.getBloquesJson().isBlank()) {
            parseAndAttachLines(block, c.getBloquesJson());
        }

        return blockRepo.save(block);
    }

    /** Modalidad de la piedra propuesta; default BOULDER si null/desconocida. */
    private static SchoolBlock.Discipline parseDiscipline(String raw) {
        if (raw == null) return SchoolBlock.Discipline.BOULDER;
        try { return SchoolBlock.Discipline.valueOf(raw.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return SchoolBlock.Discipline.BOULDER; }
    }

    /** Geometría de la piedra propuesta; default POINT si null/desconocida. */
    private static SchoolBlock.Geometry parseGeometry(String raw) {
        if (raw == null) return SchoolBlock.Geometry.POINT;
        try { return SchoolBlock.Geometry.valueOf(raw.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return SchoolBlock.Geometry.POINT; }
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
                line.setDescription(descOf(node));
                line.setVariant(variantOf(node));
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
            propagateGrade(line.getId(), grade);
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
     *
     * @return la PRIMERA vía creada (para el post NEW_LINE del feed), o null si
     *         la propuesta solo corrigió/borró vías existentes.
     */
    private BlockLineJpaEntity addLinesToExistingBlock(PendingContribution c) {
        var blockOpt = blockRepo.findById(c.getTargetBlockId());
        if (blockOpt.isEmpty()) return null;
        var block = blockOpt.get();
        if (c.getBloquesJson() == null || c.getBloquesJson().isBlank()) return null;
        BlockLineJpaEntity firstCreated = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode arr = mapper.readTree(c.getBloquesJson());
            if (!arr.isArray()) return null;
            // Ids de las vías que EXISTÍAN antes de aplicar esta propuesta (para
            // la reconciliación de borrados de abajo).
            java.util.Set<String> preexisting = new java.util.HashSet<>();
            for (var l : block.getLines()) preexisting.add(l.getId());
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
                                propagateGrade(line.getId(), grade);
                                line.setStartType(startType);
                                if (linePath != null && !linePath.isBlank()) line.setLinePath(linePath);
                                if (facePhoto != null && !facePhoto.isBlank()) line.setPhotoPath(facePhoto);
                                if (descOf(node) != null) line.setDescription(descOf(node));
                                if (variantOf(node) != null) line.setVariant(variantOf(node));
                            });
                } else {
                    String key = facePhoto == null ? "" : facePhoto;
                    Integer fo = faceOrders.get(key);
                    if (fo == null) { fo = nextFaceOrder[0]++; faceOrders.put(key, fo); }
                    BlockLineJpaEntity created = new BlockLineJpaEntity(
                            UUID.randomUUID().toString(),
                            name.isEmpty() ? String.valueOf(sortOrder + 1) : name,
                            grade, startType, linePath, sortOrder++,
                            facePhoto, fo);
                    created.setDescription(descOf(node));
                    created.setVariant(variantOf(node));
                    block.addLine(created);
                    if (firstCreated == null) firstCreated = created;
                }
            }
            // Reconciliación de BORRADOS (como en muros): si el payload trae al
            // menos un targetLineId, viene del editor unificado, que manda TODAS
            // las vías → las existentes que omite se eliminaron a propósito.
            // (Payloads sin ningún targetLineId = flujo antiguo "solo añadir" →
            // no se borra nada.)
            java.util.Set<String> keptIds = new java.util.HashSet<>();
            boolean fullEdit = false;
            for (JsonNode node : arr) {
                String tId = node.path("targetLineId").isNull() ? null : node.path("targetLineId").asText(null);
                if (tId != null && !tId.isBlank()) { fullEdit = true; keptIds.add(tId); }
            }
            if (fullEdit) {
                // Se borran las que existían antes y el payload omite; las
                // creadas en este mismo pase (sin targetLineId) se conservan.
                block.getLines().removeIf(l ->
                        preexisting.contains(l.getId()) && !keptIds.contains(l.getId()));
            }
            refreshCover(block);
            blockRepo.save(block);
        } catch (Exception ignored) { return null; }
        return firstCreated;
    }

    /**
     * Reconcilia un MURO (geometry=LINE) al estado COMPLETO propuesto, preservando
     * los ids de las vías existentes (clave para que los enganches del diario por
     * lineId sobrevivan). Las vías del payload con `lineId` conocido se ACTUALIZAN
     * en sitio; las nuevas (sin lineId) se CREAN; las existentes que el payload
     * OMITE se BORRAN (orphanRemoval). sortOrder = orden en el payload; faceOrder
     * por foto. Actualiza path/dirección/geometría del muro.
     *
     * @return la PRIMERA vía creada (para el post NEW_LINE del feed), o null si
     *         la edición no añadió vías nuevas.
     */
    private BlockLineJpaEntity reconcileWall(PendingContribution c) {
        var blockOpt = blockRepo.findById(c.getTargetBlockId());
        if (blockOpt.isEmpty()) return null;
        var block = blockOpt.get();

        SchoolBlock.Geometry geom = parseGeometry(c.getGeometry());
        block.setGeometry(geom);
        block.setPath(geom == SchoolBlock.Geometry.LINE ? c.getPath() : null);
        block.setDirection("RTL".equalsIgnoreCase(c.getDirection()) ? "RTL" : "LTR");

        if (c.getBloquesJson() == null || c.getBloquesJson().isBlank()) {
            blockRepo.save(block);
            return null;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode arr = mapper.readTree(c.getBloquesJson());
            if (!arr.isArray()) { blockRepo.save(block); return null; }

            java.util.Map<String, BlockLineJpaEntity> existingById = new java.util.HashMap<>();
            for (var l : block.getLines()) existingById.put(l.getId(), l);

            java.util.Set<String> kept = new java.util.HashSet<>();
            java.util.LinkedHashMap<String, Integer> faceOrders = new java.util.LinkedHashMap<>();
            java.util.List<BlockLineJpaEntity> toAdd = new java.util.ArrayList<>();
            int sortOrder = 0;
            for (JsonNode node : arr) {
                String lineId = textOrNull(node, "lineId");
                if (lineId == null) lineId = textOrNull(node, "targetLineId");
                String name = node.path("name").asText("").trim();
                String g = node.path("grade").isNull() ? null : node.path("grade").asText("").trim();
                String grade = (g != null && g.isEmpty()) ? null : g;
                String rawStart = node.path("startType").isNull() ? null : node.path("startType").asText("").trim();
                BlockLine.StartType startType = mapStartType(rawStart);
                String linePath = node.path("linePath").asText(null);
                String facePhoto = facePhotoOf(node, block.getPhotoPath());
                int faceOrder = faceOrders.computeIfAbsent(facePhoto == null ? "" : facePhoto,
                        k -> faceOrders.size());

                BlockLineJpaEntity line = (lineId != null) ? existingById.get(lineId) : null;
                if (line != null) {
                    // Actualiza en sitio (preserva id → enganche del diario).
                    kept.add(line.getId());
                    if (!name.isEmpty()) line.setName(name);
                    line.setGrade(grade);
                    propagateGrade(line.getId(), grade);
                    line.setStartType(startType);
                    if (linePath != null && !linePath.isBlank()) line.setLinePath(linePath);
                    line.setPhotoPath(facePhoto);
                    line.setSortOrder(sortOrder++);
                    line.setFaceOrder(faceOrder);
                    if (descOf(node) != null) line.setDescription(descOf(node));
                                if (variantOf(node) != null) line.setVariant(variantOf(node));
                } else {
                    BlockLineJpaEntity created = new BlockLineJpaEntity(
                            UUID.randomUUID().toString(),
                            name.isEmpty() ? String.valueOf(sortOrder + 1) : name,
                            grade, startType, linePath, sortOrder++, facePhoto, faceOrder);
                    created.setDescription(descOf(node));
                    created.setVariant(variantOf(node));
                    toAdd.add(created);
                }
            }
            // Borra las vías que la propuesta omite (orphanRemoval); añade las nuevas.
            block.getLines().removeIf(l -> !kept.contains(l.getId()));
            for (var l : toAdd) block.addLine(l);
            refreshCover(block);
            blockRepo.save(block);
            return toAdd.isEmpty() ? null : toAdd.get(0);
        } catch (Exception ignored) {}
        return null;
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode n = node.path(field);
        if (n.isMissingNode() || n.isNull()) return null;
        String s = n.asText(null);
        return (s == null || s.isBlank()) ? null : s;
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
                line.setDescription(descOf(node));
                line.setVariant(variantOf(node));
                block.addLine(line);
            }
        } catch (Exception ignored) {}
    }

    /** Variante opcional de la vía en el payload (null si vacía). */
    private static String variantOf(JsonNode node) {
        JsonNode v = node.path("variant");
        if (v.isMissingNode() || v.isNull()) return null;
        String t = v.asText("").trim();
        if (t.isEmpty()) return null;
        return t.length() > 60 ? t.substring(0, 60) : t;
    }

    /** Descripción opcional de la vía en el payload (null si vacía). */
    private static String descOf(JsonNode node) {
        JsonNode d = node.path("description");
        if (d.isMissingNode() || d.isNull()) return null;
        String t = d.asText("").trim();
        if (t.isEmpty()) return null;
        return t.length() > 500 ? t.substring(0, 500) : t;
    }

    private static BlockLine.StartType mapStartType(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return switch (raw.toUpperCase()) {
            case "PIE", "STAND" -> BlockLine.StartType.STAND;
            case "SIT"          -> BlockLine.StartType.SIT;
            case "SEMI"         -> BlockLine.StartType.SEMI;
            case "LANCE", "JUMP" -> BlockLine.StartType.JUMP;
            case "TRAV"         -> BlockLine.StartType.TRAV;
            default             -> null;
        };
    }
}
