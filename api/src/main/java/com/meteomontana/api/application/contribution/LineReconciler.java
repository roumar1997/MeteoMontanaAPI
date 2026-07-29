package com.meteomontana.api.application.contribution;

import com.meteomontana.api.application.contribution.ContributionLineParser.ParsedLine;
import com.meteomontana.api.domain.model.PendingContribution;
import com.meteomontana.api.domain.model.SchoolBlock;
import com.meteomontana.api.infrastructure.persistence.jpa.BlockLineJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SchoolBlockJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataJournalRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataSchoolBlockRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;

/**
 * Aplica una contribución BOULDER sobre un bloque EXISTENTE: corregir una vía
 * concreta, añadir/corregir/borrar vías (editor unificado) y reconciliar muros
 * completos preservando los ids (los enganches del diario por lineId
 * sobreviven). Extraído de ReviewContributionUseCase (SRP).
 */
@Service
@RequiredArgsConstructor
public class LineReconciler {

    private final SpringDataSchoolBlockRepository blockRepo;
    private final SpringDataJournalRepository journalRepo;

    /** Propaga el grado nuevo de una vía al diario de todos (si la vía tiene id). */
    private void propagateGrade(String lineId, String grade) {
        if (lineId != null && !lineId.isBlank()) journalRepo.updateGradeByLineId(lineId, grade);
    }

    /** Campos comunes de una corrección sobre una vía existente. La foto la
     *  decide cada rama (el muro la fija siempre; las demás solo si viene). */
    private void applyCorrection(BlockLineJpaEntity line, ParsedLine p) {
        if (!p.name().isEmpty()) line.setName(p.name());
        line.setGrade(p.grade());
        propagateGrade(line.getId(), p.grade());
        line.setStartType(p.startType());
        if (p.linePath() != null && !p.linePath().isBlank()) line.setLinePath(p.linePath());
        // BUG cazado 2026-07-18: la rama de "✎ CORREGIR VÍA" no aplicaba
        // descripción NI variante — con el parser único ya no puede divergir.
        if (p.description() != null) line.setDescription(p.description());
        if (p.variant() != null) line.setVariant(p.variant());
    }

    /**
     * Actualiza una línea existente con los datos del primer bloque del JSON.
     * Usado para correcciones de vía (targetBlockId + targetLineId != null).
     */
    public void updateExistingLine(PendingContribution c) {
        var blockOpt = blockRepo.findById(c.getTargetBlockId());
        if (blockOpt.isEmpty()) return;
        var block = blockOpt.get();
        var lineOpt = block.getLines().stream()
                .filter(l -> l.getId().equals(c.getTargetLineId()))
                .findFirst();
        if (lineOpt.isEmpty()) return;
        var line = lineOpt.get();

        var parsed = ContributionLineParser.parse(c.getBloquesJson(), block.getPhotoPath());
        if (parsed.isEmpty()) return;
        ParsedLine p = parsed.get(0);
        applyCorrection(line, p);
        if (p.facePhoto() != null && !p.facePhoto().isBlank()) line.setPhotoPath(p.facePhoto());
        BlockMaterializer.refreshCover(block);
        blockRepo.save(block);
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
    public BlockLineJpaEntity addLinesToExistingBlock(PendingContribution c) {
        var blockOpt = blockRepo.findById(c.getTargetBlockId());
        if (blockOpt.isEmpty()) return null;
        var block = blockOpt.get();
        var parsed = ContributionLineParser.parse(c.getBloquesJson(), block.getPhotoPath());
        if (parsed.isEmpty()) return null;
        BlockLineJpaEntity firstCreated = null;

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
        int nextFaceOrder = faceOrders.values().stream()
                .mapToInt(Integer::intValue).max().orElse(-1) + 1;

        for (ParsedLine p : parsed) {
            if (p.targetLineId() != null) {
                // Corrige una vía existente de este mismo bloque.
                block.getLines().stream()
                        .filter(l -> l.getId().equals(p.targetLineId())).findFirst()
                        .ifPresent(line -> {
                            applyCorrection(line, p);
                            if (p.facePhoto() != null && !p.facePhoto().isBlank())
                                line.setPhotoPath(p.facePhoto());
                        });
            } else {
                String key = p.facePhoto() == null ? "" : p.facePhoto();
                Integer fo = faceOrders.get(key);
                if (fo == null) { fo = nextFaceOrder++; faceOrders.put(key, fo); }
                BlockLineJpaEntity created = new BlockLineJpaEntity(
                        UUID.randomUUID().toString(),
                        p.name().isEmpty() ? String.valueOf(sortOrder + 1) : p.name(),
                        p.grade(), p.startType(), p.linePath(), sortOrder++,
                        p.facePhoto(), fo);
                created.setDescription(p.description());
                created.setVariant(p.variant());
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
        for (ParsedLine p : parsed) {
            if (p.targetLineId() != null) { fullEdit = true; keptIds.add(p.targetLineId()); }
        }
        if (fullEdit) {
            // Se borran las que existían antes y el payload omite; las
            // creadas en este mismo pase (sin targetLineId) se conservan.
            block.getLines().removeIf(l ->
                    preexisting.contains(l.getId()) && !keptIds.contains(l.getId()));
        }
        BlockMaterializer.refreshCover(block);
        blockRepo.save(block);
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
    public BlockLineJpaEntity reconcileWall(PendingContribution c) {
        var blockOpt = blockRepo.findById(c.getTargetBlockId());
        if (blockOpt.isEmpty()) return null;
        var block = blockOpt.get();

        SchoolBlock.Geometry geom = ContributionLineParser.parseGeometry(c.getGeometry());
        block.setGeometry(geom);
        block.setPath(geom == SchoolBlock.Geometry.LINE ? c.getPath() : null);
        block.setDirection("RTL".equalsIgnoreCase(c.getDirection()) ? "RTL" : "LTR");

        var parsed = ContributionLineParser.parse(c.getBloquesJson(), block.getPhotoPath());
        if (parsed.isEmpty()) {
            blockRepo.save(block);
            return null;
        }
        java.util.Map<String, BlockLineJpaEntity> existingById = new java.util.HashMap<>();
        for (var l : block.getLines()) existingById.put(l.getId(), l);

        java.util.Set<String> kept = new java.util.HashSet<>();
        java.util.LinkedHashMap<String, Integer> faceOrders = new java.util.LinkedHashMap<>();
        java.util.List<BlockLineJpaEntity> toAdd = new java.util.ArrayList<>();
        int sortOrder = 0;
        for (ParsedLine p : parsed) {
            int faceOrder = faceOrders.computeIfAbsent(
                    p.facePhoto() == null ? "" : p.facePhoto(), k -> faceOrders.size());
            BlockLineJpaEntity line = (p.targetLineId() != null)
                    ? existingById.get(p.targetLineId()) : null;
            if (line != null) {
                // Actualiza en sitio (preserva id → enganche del diario).
                kept.add(line.getId());
                applyCorrection(line, p);
                line.setPhotoPath(p.facePhoto());
                line.setSortOrder(sortOrder++);
                line.setFaceOrder(faceOrder);
            } else {
                BlockLineJpaEntity created = new BlockLineJpaEntity(
                        UUID.randomUUID().toString(),
                        p.name().isEmpty() ? String.valueOf(sortOrder + 1) : p.name(),
                        p.grade(), p.startType(), p.linePath(), sortOrder++,
                        p.facePhoto(), faceOrder);
                created.setDescription(p.description());
                created.setVariant(p.variant());
                toAdd.add(created);
            }
        }
        // Borra las vías que la propuesta omite (orphanRemoval); añade las nuevas.
        block.getLines().removeIf(l -> !kept.contains(l.getId()));
        for (var l : toAdd) block.addLine(l);
        BlockMaterializer.refreshCover(block);
        blockRepo.save(block);
        return toAdd.isEmpty() ? null : toAdd.get(0);
    }
}
