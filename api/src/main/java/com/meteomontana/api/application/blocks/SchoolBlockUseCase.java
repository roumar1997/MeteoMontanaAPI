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
            String name, String grade, String startType, String linePath
    ) {}

    public record CreateBlockRequest(
            String type,            // BLOCK / PARKING / ZONE
            String name,
            Double lat,
            Double lon,
            String photoPath,
            String description,
            List<CreateBlockLineRequest> lines,
            String sectorBlockId    // BLOCK: id del sector (ZONE) al que pertenece (opcional)
    ) {}

    public record BlockDto(
            String id, String schoolId, String type, String name,
            double lat, double lon, String photoPath, String description,
            String createdByUid, String createdAt, List<BlockLineDto> lines,
            String sectorBlockId
    ) {}

    public record BlockLineDto(
            String id, String name, String grade, String startType,
            String linePath, int sortOrder
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
                        req.lines().indexOf(l)
                )).toList();

        SchoolBlock block = new SchoolBlock(
                UUID.randomUUID().toString(),
                schoolId, type, name,
                req.lat(), req.lon(),
                req.photoPath(),
                req.description(),
                creatorUid, LocalDateTime.now(), lines,
                type == SchoolBlock.Type.BLOCK ? req.sectorBlockId() : null
        );
        return toDto(blockRepository.save(block));
    }

    /** Siguiente número de piedra libre en la escuela (máx número existente + 1). */
    private String nextBlockNumber(String schoolId) {
        int max = blockRepository.findBySchoolId(schoolId).stream()
                .filter(b -> b.getType() == SchoolBlock.Type.BLOCK)
                .map(SchoolBlock::getName)
                .filter(n -> n != null && n.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .max().orElse(0);
        return String.valueOf(max + 1);
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
                        req.lines().indexOf(l)
                )).toList();

        SchoolBlock updated = new SchoolBlock(
                blockId, current.getSchoolId(), type,
                req.name() != null ? req.name() : current.getName(),
                req.lat() != null ? req.lat() : current.getLat(),
                req.lon() != null ? req.lon() : current.getLon(),
                req.photoPath() != null ? req.photoPath() : current.getPhotoPath(),
                req.description() != null ? req.description() : current.getDescription(),
                current.getCreatedByUid(), current.getCreatedAt(), lines,
                req.sectorBlockId() != null ? req.sectorBlockId() : current.getSectorBlockId()
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
        return new BlockDto(
                b.getId(), b.getSchoolId(), b.getType().name(), b.getName(),
                b.getLat(), b.getLon(), b.getPhotoPath(), b.getDescription(),
                b.getCreatedByUid(), b.getCreatedAt().toString(),
                b.getLines().stream().map(l -> new BlockLineDto(
                        l.getId(), l.getName(), l.getGrade(),
                        l.getStartType() != null ? l.getStartType().name() : null,
                        l.getLinePath(), l.getSortOrder()
                )).toList(),
                b.getSectorBlockId()
        );
    }
}
