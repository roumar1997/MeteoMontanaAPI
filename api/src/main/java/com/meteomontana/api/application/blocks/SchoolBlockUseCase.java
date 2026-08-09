package com.meteomontana.api.application.blocks;

import com.meteomontana.api.domain.exception.ForbiddenException;
import com.meteomontana.api.domain.exception.SchoolNotFoundException;
import com.meteomontana.api.domain.model.BlockLine;
import com.meteomontana.api.domain.model.SchoolBlock;
import com.meteomontana.api.domain.port.SchoolBlockRepository;
import com.meteomontana.api.domain.port.SchoolRepository;
import com.meteomontana.api.domain.port.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SchoolBlockUseCase {

    public record CreateBlockLineRequest(
            /**
             * Id de la via EXISTENTE que representa esta fila; null si es nueva.
             * Las apps desde 2.21.3 lo mandan y entonces no hay nada que
             * emparejar. Las apps viejas no lo mandan y se sigue adivinando.
             */
            String id,
            String name, String grade, String startType, String linePath,
            String photoPath,   // cara (foto) sobre la que está dibujada esta vía (opcional)
            Integer faceOrder,  // orden de la cara (opcional, default 0)
            String description, // descripción/beta opcional de la vía
            String variant      // variante opcional ("directa", "extensión"...)
    ) {}

    public record CreateBlockRequest(
            String type,            // BLOCK / PARKING / ZONE
            String name,
            Double lat,
            Double lon,
            String photoPath,
            String description,
            List<CreateBlockLineRequest> lines,
            String sectorBlockId,   // BLOCK: id del sector (ZONE) al que pertenece (opcional)
            String discipline,      // BLOCK: BOULDER (bloque) / ROUTE (vía). Default BOULDER.
            String geometry,        // BLOCK: POINT (marcador) / LINE (muro). Default POINT.
            String path,            // BLOCK+LINE: polilínea JSON [[lat,lon],...]
            String direction        // BLOCK+LINE: "LTR"/"RTL" (numeración del muro)
    ) {}

    public record BlockDto(
            String id, String schoolId, String type, String name,
            double lat, double lon, String photoPath, String description,
            String createdByUid, String createdAt, List<BlockLineDto> lines,
            String sectorBlockId,
            String discipline,      // BOULDER (bloque) / ROUTE (vía)
            String geometry,        // POINT / LINE
            String path,            // polilínea JSON si LINE
            String direction,       // "LTR"/"RTL"
            // Caras = la piedra agrupada por foto (cada cara: foto + sus vías).
            // `lines` y `photoPath` se mantienen (= primera cara) por compat.
            List<BlockFaceDto> faces
    ) {}

    public record BlockLineDto(
            String id, String name, String grade, String startType,
            String linePath, int sortOrder, String photoPath, int faceOrder,
            Float avgStars,     // media de valoraciones (null si nadie ha valorado)
            Integer myStars,    // valoración del usuario actual (null si no ha valorado)
            String description, // beta/detalle opcional de la vía
            String variant      // variante opcional ("directa", "extensión"...)
    ) {
        // Constructor de compatibilidad para código existente sin ratings
        public BlockLineDto(String id, String name, String grade, String startType,
                            String linePath, int sortOrder, String photoPath, int faceOrder) {
            this(id, name, grade, startType, linePath, sortOrder, photoPath, faceOrder, null, null, null, null);
        }
    }

    /** Una cara de la piedra: una foto y las vías dibujadas sobre ella. */
    public record BlockFaceDto(
            String photoPath, int sortOrder, List<BlockLineDto> lines
    ) {}

    private final SchoolBlockRepository blockRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final com.meteomontana.api.domain.port.LineRatingRepository ratingRepo;
    /**
     * El diario guarda el nombre de la vía COPIADO, así que al renombrarla hay
     * que propagarlo o el perfil de todo el mundo se queda con el viejo (y deja
     * de llevar a la piedra al pulsarlo).
     */
    private final com.meteomontana.api.domain.port.JournalRepository journalRepo;

    public List<BlockDto> listBySchool(String schoolId) {
        return listBySchool(schoolId, null);
    }

    public List<BlockDto> listBySchool(String schoolId, String callerUid) {
        var blocks = blockRepository.findBySchoolId(schoolId);
        // Ratings EN LOTE: antes eran 2 queries POR VÍA (media + mi voto) →
        // ~400 queries al abrir una escuela grande. Ahora 2 por escuela.
        var lineIds = blocks.stream()
                .flatMap(b -> b.getLines().stream())
                .map(com.meteomontana.api.domain.model.BlockLine::getId)
                .toList();
        var avgByLine = new java.util.HashMap<String, Double>();
        var myByLine = new java.util.HashMap<String, Integer>();
        if (!lineIds.isEmpty()) {
            avgByLine.putAll(ratingRepo.avgStarsByLineIds(lineIds));
            if (callerUid != null) {
                myByLine.putAll(ratingRepo.myStarsByLineIds(callerUid, lineIds));
            }
        }
        return blocks.stream().map(b -> toDto(b, avgByLine, myByLine)).toList();
    }

    public BlockDto findById(String id) {
        return blockRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> new SchoolNotFoundException(id));
    }

    @Transactional
    public BlockDto create(String creatorUid, String schoolId, CreateBlockRequest req) {
        schoolRepository.findById(schoolId)
                .orElseThrow(() -> new SchoolNotFoundException(schoolId));

        if (req.lat() == null || req.lon() == null)
            throw new IllegalArgumentException("lat/lon required");
        SchoolBlock.Type type = SchoolBlock.Type.valueOf(req.type());
        // PIEDRA (BLOCK): número automático único en la escuela (sin nombre libre).
        // PARKING/ZONE: nombre propio (obligatorio).
        String name;
        if (type == SchoolBlock.Type.BLOCK) {
            name = nextBlockNumber(schoolId);
        } else {
            if (req.name() == null || req.name().isBlank())
                throw new IllegalArgumentException("name required");
            name = req.name().trim();
        }

        List<BlockLine> lines = req.lines() == null ? List.of() :
                req.lines().stream().map(l -> {
                    BlockLine bl = new BlockLine(
                            UUID.randomUUID().toString(),
                            "",
                            l.name(),
                            l.grade(),
                            l.startType() != null ? BlockLine.StartType.valueOf(l.startType()) : null,
                            l.linePath(),
                            req.lines().indexOf(l),
                            l.photoPath() != null ? l.photoPath() : req.photoPath(),
                            l.faceOrder() != null ? l.faceOrder() : 0);
                    bl.setDescription(trimDesc(l.description()));
                    bl.setVariant(trimVariant(l.variant()));
                    return bl;
                }).toList();

        SchoolBlock.Discipline discipline = type == SchoolBlock.Type.BLOCK
                ? com.meteomontana.api.application.contribution.ContributionLineParser.parseDiscipline(req.discipline()) : SchoolBlock.Discipline.BOULDER;
        SchoolBlock.Geometry geometry = type == SchoolBlock.Type.BLOCK
                ? com.meteomontana.api.application.contribution.ContributionLineParser.parseGeometry(req.geometry()) : SchoolBlock.Geometry.POINT;
        SchoolBlock block = new SchoolBlock(
                UUID.randomUUID().toString(),
                schoolId, type, discipline, name,
                req.lat(), req.lon(),
                req.photoPath(),
                req.description(),
                creatorUid, LocalDateTime.now(), lines,
                type == SchoolBlock.Type.BLOCK ? req.sectorBlockId() : null,
                geometry,
                geometry == SchoolBlock.Geometry.LINE ? req.path() : null,
                parseDirection(req.direction())
        );
        return toDto(blockRepository.save(block));
    }

    /** Normaliza la variante: null si vacía, recortada a 60. */
    private static String trimVariant(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        if (t.isEmpty()) return null;
        return t.length() > 60 ? t.substring(0, 60) : t;
    }

    /** Normaliza la descripción de vía: null si vacía, recortada a 500. */
    private static String trimDesc(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String t = raw.trim();
        return t.length() > 500 ? t.substring(0, 500) : t;
    }

    /** Normaliza la dirección del muro a LTR/RTL; default LTR. */
    private String parseDirection(String raw) {
        return "RTL".equalsIgnoreCase(raw != null ? raw.trim() : null) ? "RTL" : "LTR";
    }

    /** Menor número de piedra LIBRE en la escuela (rellena huecos al borrar).
     *  Único por escuela. */
    private String nextBlockNumber(String schoolId) {
        java.util.Set<Integer> used = blockRepository.findBySchoolId(schoolId).stream()
                .filter(b -> b.getType() == SchoolBlock.Type.BLOCK)
                .map(SchoolBlock::getName)
                .filter(n -> n != null && n.matches("\\d+"))
                .map(Integer::parseInt)
                .collect(java.util.stream.Collectors.toSet());
        int n = 1;
        while (used.contains(n)) n++;
        return String.valueOf(n);
    }

    @Transactional
    public BlockDto update(String editorUid, String blockId, CreateBlockRequest req) {
        SchoolBlock current = blockRepository.findById(blockId)
                .orElseThrow(() -> new SchoolNotFoundException(blockId));
        boolean isOwner = current.getCreatedByUid().equals(editorUid);
        boolean isAdmin = userRepository.findByUid(editorUid)
                .map(u -> u.isAdmin()).orElse(false);
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("Solo el creador o un admin pueden editar este bloque");
        }

        SchoolBlock.Type type = req.type() != null ? SchoolBlock.Type.valueOf(req.type()) : current.getType();
        // Cada vía conserva SU id: del id cuelgan el ✓ del diario de cada
        // usuario, las estrellas, los votos de grado, los comentarios, los
        // posts del feed y los enlaces compartidos. El editor manda la lista
        // entera sin decir cuál era cuál, así que hay que emparejarlas.
        List<String> ids = req.lines() == null ? List.of()
                : BlockLineIdReconciler.assignIds(current.getLines(),
                        req.lines().stream()
                                .map(l -> new BlockLineIdReconciler.Incoming(
                                        l.id(), l.name(), l.faceOrder()))
                                .toList());
        // Por ÍNDICE, no por indexOf: dos vías con los mismos datos son
        // registros iguales y indexOf devolvía siempre la primera, así que
        // compartían id y orden.
        List<BlockLine> lines;
        if (req.lines() == null) {
            lines = current.getLines();
        } else {
            List<BlockLine> nuevas = new java.util.ArrayList<>(req.lines().size());
            for (int i = 0; i < req.lines().size(); i++) {
                var l = req.lines().get(i);
                BlockLine bl = new BlockLine(
                        ids.get(i),
                        blockId,
                        l.name(),
                        l.grade(),
                        l.startType() != null ? BlockLine.StartType.valueOf(l.startType()) : null,
                        l.linePath(),
                        i,
                        l.photoPath() != null ? l.photoPath() : req.photoPath(),
                        l.faceOrder() != null ? l.faceOrder() : 0);
                bl.setDescription(trimDesc(l.description()));
                bl.setVariant(trimVariant(l.variant()));
                nuevas.add(bl);
            }
            lines = List.copyOf(nuevas);
        }

        SchoolBlock.Discipline discipline = req.discipline() != null
                ? com.meteomontana.api.application.contribution.ContributionLineParser.parseDiscipline(req.discipline()) : current.getDiscipline();
        SchoolBlock.Geometry geometry = req.geometry() != null
                ? com.meteomontana.api.application.contribution.ContributionLineParser.parseGeometry(req.geometry()) : current.getGeometry();
        String path = req.geometry() != null
                ? (geometry == SchoolBlock.Geometry.LINE ? req.path() : null)
                : current.getPath();
        String direction = req.direction() != null ? parseDirection(req.direction()) : current.getDirection();
        SchoolBlock updated = new SchoolBlock(
                blockId, current.getSchoolId(), type, discipline,
                req.name() != null ? req.name() : current.getName(),
                req.lat() != null ? req.lat() : current.getLat(),
                req.lon() != null ? req.lon() : current.getLon(),
                req.photoPath() != null ? req.photoPath() : current.getPhotoPath(),
                req.description() != null ? req.description() : current.getDescription(),
                current.getCreatedByUid(), current.getCreatedAt(), lines,
                req.sectorBlockId() != null ? req.sectorBlockId() : current.getSectorBlockId(),
                geometry, path, direction
        );
        // NUNCA se borra la fila para editar: guardar encima ya reconcilia las
        // vías (orphanRemoval quita las que el editor omitió). Borrar tenía dos
        // efectos colaterales graves, los dos cazados el 2026-08-05:
        //  · en un SECTOR disparaba el ON DELETE SET NULL de sector_block_id y
        //    sus piedras se quedaban sueltas, y reinsertar la fila con el mismo
        //    id no las recupera;
        //  · en una PIEDRA borraba las filas de sus vías, y con ellas —por
        //    cascade— las estrellas y los votos de grado de todo el mundo.
        SchoolBlock guardado = blockRepository.save(updated);
        propagarNombresAlDiario(current.getLines(), lines);
        return toDto(guardado);
    }

    /**
     * Lleva al diario los nombres de las vías que se han renombrado. Solo las
     * que conservan su id: una vía nueva no tiene entradas que actualizar.
     */
    private void propagarNombresAlDiario(List<BlockLine> antes, List<BlockLine> despues) {
        if (antes == null || despues == null) return;
        java.util.Map<String, String> nombreAntes = new java.util.HashMap<>();
        for (BlockLine l : antes) nombreAntes.put(l.getId(), l.getName());
        for (BlockLine l : despues) {
            String viejo = nombreAntes.get(l.getId());
            if (viejo != null && l.getName() != null && !l.getName().isBlank()
                    && !viejo.equals(l.getName())) {
                journalRepo.updateNameByLineId(l.getId(), l.getName());
            }
        }
    }

    /**
     * Borra un bloque. Permitido si es el creador O si es admin.
     * La BD borra las líneas en cascada (FK con ON DELETE CASCADE).
     */
    @Transactional
    public void delete(String requesterUid, String blockId) {
        SchoolBlock b = blockRepository.findById(blockId)
                .orElseThrow(() -> new SchoolNotFoundException(blockId));
        boolean isOwner = b.getCreatedByUid().equals(requesterUid);
        boolean isAdmin = userRepository.findByUid(requesterUid)
                .map(u -> u.isAdmin()).orElse(false);
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("Solo el creador o un admin pueden borrar este bloque");
        }
        blockRepository.deleteById(blockId);
    }

    private BlockDto toDto(SchoolBlock b) {
        // Un solo bloque: el lote sigue mereciendo la pena (2 queries fijas).
        var lineIds = b.getLines().stream()
                .map(com.meteomontana.api.domain.model.BlockLine::getId).toList();
        var avgByLine = new java.util.HashMap<String, Double>();
        if (!lineIds.isEmpty()) {
            avgByLine.putAll(ratingRepo.avgStarsByLineIds(lineIds));
        }
        return toDto(b, avgByLine, java.util.Map.of());
    }

    private BlockDto toDto(SchoolBlock b,
                           java.util.Map<String, Double> avgByLine,
                           java.util.Map<String, Integer> myByLine) {
        List<BlockLineDto> lines = b.getLines().stream().map(l -> {
            Double avg = avgByLine.get(l.getId());
            Integer my = myByLine.get(l.getId());
            return new BlockLineDto(
                    l.getId(), l.getName(), l.getGrade(),
                    l.getStartType() != null ? l.getStartType().name() : null,
                    l.getLinePath(), l.getSortOrder(),
                    l.getPhotoPath() != null ? l.getPhotoPath() : b.getPhotoPath(),
                    l.getFaceOrder(),
                    avg == null ? null : avg.floatValue(),
                    my,
                    l.getDescription(),
                    l.getVariant()
            );
        }).toList();
        return new BlockDto(
                b.getId(), b.getSchoolId(), b.getType().name(), b.getName(),
                b.getLat(), b.getLon(), b.getPhotoPath(), b.getDescription(),
                b.getCreatedByUid(), b.getCreatedAt().toString(),
                lines, b.getSectorBlockId(),
                b.getDiscipline().name(),
                b.getGeometry().name(), b.getPath(), b.getDirection(),
                buildFaces(lines, b.getPhotoPath())
        );
    }

    /**
     * Agrupa las vías por foto en CARAS, preservando el orden de aparición.
     * Las vías sin foto caen en la cara de la foto de portada de la piedra.
     * Si la piedra no tiene foto ni ninguna vía, no hay caras.
     */
    private static List<BlockFaceDto> buildFaces(List<BlockLineDto> lines, String coverPhoto) {
        java.util.LinkedHashMap<String, List<BlockLineDto>> byPhoto = new java.util.LinkedHashMap<>();
        for (BlockLineDto l : lines) {
            String key = l.photoPath() != null ? l.photoPath()
                    : (coverPhoto != null ? coverPhoto : "");
            byPhoto.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(l);
        }
        List<BlockFaceDto> faces = new java.util.ArrayList<>();
        int order = 0;
        for (var entry : byPhoto.entrySet()) {
            String photo = entry.getKey().isEmpty() ? null : entry.getKey();
            faces.add(new BlockFaceDto(photo, order++, entry.getValue()));
        }
        return faces;
    }
}
