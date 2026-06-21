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

@Service
public class SchoolBlockUseCase {

    public record CreateBlockLineRequest(
            String name, String grade, String startType, String linePath,
            String photoPath,   // cara (foto) sobre la que está dibujada esta vía (opcional)
            Integer faceOrder   // orden de la cara (opcional, default 0)
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
            String linePath, int sortOrder, String photoPath, int faceOrder
    ) {}

    /** Una cara de la piedra: una foto y las vías dibujadas sobre ella. */
    public record BlockFaceDto(
            String photoPath, int sortOrder, List<BlockLineDto> lines
    ) {}

    private final SchoolBlockRepository blockRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;

    public SchoolBlockUseCase(SchoolBlockRepository blockRepository,
                              SchoolRepository schoolRepository,
                              UserRepository userRepository) {
        this.blockRepository = blockRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
    }

    public List<BlockDto> listBySchool(String schoolId) {
        return blockRepository.findBySchoolId(schoolId).stream().map(this::toDto).toList();
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
                req.lines().stream().map(l -> new BlockLine(
                        UUID.randomUUID().toString(),
                        "",
                        l.name(),
                        l.grade(),
                        l.startType() != null ? BlockLine.StartType.valueOf(l.startType()) : null,
                        l.linePath(),
                        req.lines().indexOf(l),
                        l.photoPath() != null ? l.photoPath() : req.photoPath(),
                        l.faceOrder() != null ? l.faceOrder() : 0
                )).toList();

        SchoolBlock.Discipline discipline = type == SchoolBlock.Type.BLOCK
                ? parseDiscipline(req.discipline()) : SchoolBlock.Discipline.BOULDER;
        SchoolBlock.Geometry geometry = type == SchoolBlock.Type.BLOCK
                ? parseGeometry(req.geometry()) : SchoolBlock.Geometry.POINT;
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

    /** Parsea la modalidad recibida del cliente; default BOULDER si null/desconocida. */
    private SchoolBlock.Discipline parseDiscipline(String raw) {
        if (raw == null) return SchoolBlock.Discipline.BOULDER;
        try { return SchoolBlock.Discipline.valueOf(raw.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return SchoolBlock.Discipline.BOULDER; }
    }

    /** Parsea la geometría; default POINT si null/desconocida. */
    private SchoolBlock.Geometry parseGeometry(String raw) {
        if (raw == null) return SchoolBlock.Geometry.POINT;
        try { return SchoolBlock.Geometry.valueOf(raw.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return SchoolBlock.Geometry.POINT; }
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
        List<BlockLine> lines = req.lines() == null ? current.getLines() :
                req.lines().stream().map(l -> new BlockLine(
                        UUID.randomUUID().toString(),
                        blockId,
                        l.name(),
                        l.grade(),
                        l.startType() != null ? BlockLine.StartType.valueOf(l.startType()) : null,
                        l.linePath(),
                        req.lines().indexOf(l),
                        l.photoPath() != null ? l.photoPath() : req.photoPath(),
                        l.faceOrder() != null ? l.faceOrder() : 0
                )).toList();

        SchoolBlock.Discipline discipline = req.discipline() != null
                ? parseDiscipline(req.discipline()) : current.getDiscipline();
        SchoolBlock.Geometry geometry = req.geometry() != null
                ? parseGeometry(req.geometry()) : current.getGeometry();
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
        // Borramos el viejo y guardamos nuevo (cascade eliminará líneas viejas)
        blockRepository.deleteById(blockId);
        return toDto(blockRepository.save(updated));
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
        List<BlockLineDto> lines = b.getLines().stream().map(l -> new BlockLineDto(
                l.getId(), l.getName(), l.getGrade(),
                l.getStartType() != null ? l.getStartType().name() : null,
                l.getLinePath(), l.getSortOrder(),
                l.getPhotoPath() != null ? l.getPhotoPath() : b.getPhotoPath(),
                l.getFaceOrder()
        )).toList();
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
