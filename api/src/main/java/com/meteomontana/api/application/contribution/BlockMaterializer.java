package com.meteomontana.api.application.contribution;

import com.meteomontana.api.domain.model.PendingContribution;
import com.meteomontana.api.domain.model.SchoolBlock;
import com.meteomontana.api.infrastructure.persistence.jpa.BlockLineJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SchoolBlockJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataSchoolBlockRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meteomontana.api.domain.model.CommunityVotes.OrientationVote;
import com.meteomontana.api.domain.port.CommunityVoteRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/**
 * Materializa un school_block NUEVO al aprobar una contribución
 * (PARKING/BOULDER/SECTOR): numeración de piedras, vías del payload repartidas
 * en caras y portada. Extraído de ReviewContributionUseCase (SRP).
 */
@Service
@RequiredArgsConstructor
public class BlockMaterializer {

    private final SpringDataSchoolBlockRepository blockRepo;
    private final CommunityVoteRepository votes;

    public SchoolBlockJpaEntity createBlock(PendingContribution c, SchoolBlock.Type type, String adminUid) {
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
            block.setDiscipline(ContributionLineParser.parseDiscipline(c.getDiscipline()));
            // Geometría: punto o muro (polilínea).
            SchoolBlock.Geometry geom = ContributionLineParser.parseGeometry(c.getGeometry());
            block.setGeometry(geom);
            block.setPath(geom == SchoolBlock.Geometry.LINE ? c.getPath() : null);
            block.setDirection("RTL".equalsIgnoreCase(c.getDirection()) ? "RTL" : "LTR");
        }

        // Para BOULDER: parsear bloquesJson y crear las líneas (vías) del bloque.
        // Cascade ALL del @OneToMany hace que se persistan al guardar el SchoolBlockJpaEntity.
        if (type == SchoolBlock.Type.BLOCK) {
            attachLines(block, c.getBloquesJson());
        }

        var saved = blockRepo.save(block);
        // Orientación del autor (opcional al proponer): su PRIMER voto.
        // El consenso comunitario sigue mandando después (más votos lo mueven).
        for (OrientationVote v : parseAuthorOrientations(
                c.getOrientationsJson(), saved.getId(), c.getSubmittedByUid())) {
            votes.upsertOrientationVote(v);
        }
        return saved;
    }

    private static final Set<String> VALID_ASPECTS =
            Set.of("N", "NE", "E", "SE", "S", "SO", "O", "NO");

    /**
     * Parsea {"block":"NE","faces":{"0":"N","2":"S"}} a votos del autor.
     * Tolerante: JSON inválido, rumbos desconocidos o índices no numéricos
     * se ignoran (la propuesta se aprueba igual; los votos son un extra).
     */
    static List<OrientationVote> parseAuthorOrientations(String json, String blockId, String uid) {
        List<OrientationVote> out = new ArrayList<>();
        if (json == null || json.isBlank() || uid == null) return out;
        try {
            JsonNode root = new ObjectMapper().readTree(json);
            String whole = root.path("block").asText(null);
            if (whole != null && VALID_ASPECTS.contains(whole)) {
                out.add(new OrientationVote(blockId, null, uid, whole));
            }
            JsonNode faces = root.path("faces");
            if (faces.isObject()) {
                faces.properties().forEach(e2 -> {
                    try {
                        int idx = Integer.parseInt(e2.getKey());
                        String aspect = e2.getValue().asText("");
                        if (idx >= 0 && VALID_ASPECTS.contains(aspect)) {
                            out.add(new OrientationVote(blockId, idx, uid, aspect));
                        }
                    } catch (NumberFormatException ignored) { }
                });
            }
        } catch (Exception ignored) { }
        return out;
    }

    /**
     * Añade al bloque las vías del payload. Cada vía puede traer su propia
     * `photoUrl` (la CARA sobre la que está dibujada); se reparten en caras
     * agrupando por foto, con `faceOrder` según orden de aparición. Sin
     * `photoUrl` (piedra de una sola foto) caen todas en la portada (cara 0).
     */
    private void attachLines(SchoolBlockJpaEntity block, String bloquesJson) {
        var parsed = ContributionLineParser.parse(bloquesJson, block.getPhotoPath());
        LinkedHashMap<String, Integer> faceOrders = new LinkedHashMap<>();
        int sortOrder = 0;
        for (var p : parsed) {
            int faceOrder = faceOrders.computeIfAbsent(
                    p.facePhoto() == null ? "" : p.facePhoto(), k -> faceOrders.size());
            BlockLineJpaEntity line = new BlockLineJpaEntity(
                    UUID.randomUUID().toString(),
                    p.name().isEmpty() ? String.valueOf(sortOrder + 1) : p.name(),
                    p.grade(),
                    p.startType(),
                    p.linePath(),
                    sortOrder++,
                    p.facePhoto(),
                    faceOrder
            );
            line.setDescription(p.description());
            line.setVariant(p.variant());
            block.addLine(line);
        }
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
     * La portada del bloque = foto de la CARA 0 (menor faceOrder, luego sortOrder).
     * Tras corregir/añadir vías (que pueden cambiar la foto de una cara), mantiene
     * la portada al día para miniaturas y marcadores del mapa.
     */
    static void refreshCover(SchoolBlockJpaEntity block) {
        block.getLines().stream()
                .filter(l -> l.getPhotoPath() != null && !l.getPhotoPath().isBlank())
                .min(java.util.Comparator
                        .comparingInt(BlockLineJpaEntity::getFaceOrder)
                        .thenComparingInt(BlockLineJpaEntity::getSortOrder))
                .ifPresent(l -> block.setPhotoPath(l.getPhotoPath()));
    }
}
