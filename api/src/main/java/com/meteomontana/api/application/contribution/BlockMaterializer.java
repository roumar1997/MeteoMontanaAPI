package com.meteomontana.api.application.contribution;

import com.meteomontana.api.domain.model.PendingContribution;
import com.meteomontana.api.domain.model.SchoolBlock;
import com.meteomontana.api.infrastructure.persistence.jpa.BlockLineJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SchoolBlockJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataSchoolBlockRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.UUID;

/**
 * Materializa un school_block NUEVO al aprobar una contribución
 * (PARKING/BOULDER/SECTOR): numeración de piedras, vías del payload repartidas
 * en caras y portada. Extraído de ReviewContributionUseCase (SRP).
 */
@Service
public class BlockMaterializer {

    private final SpringDataSchoolBlockRepository blockRepo;

    public BlockMaterializer(SpringDataSchoolBlockRepository blockRepo) {
        this.blockRepo = blockRepo;
    }

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

        return blockRepo.save(block);
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
