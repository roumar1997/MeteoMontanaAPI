package com.meteomontana.api.application.blocks;

import com.meteomontana.api.domain.exception.ForbiddenException;
import com.meteomontana.api.domain.exception.SchoolNotFoundException;
import com.meteomontana.api.domain.model.BlockLine;
import com.meteomontana.api.domain.model.SchoolBlock;
import com.meteomontana.api.domain.port.SchoolBlockRepository;
import com.meteomontana.api.domain.port.SchoolRepository;
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
            List<CreateBlockLineRequest> lines
    ) {}

    public record BlockDto(
            String id, String schoolId, String type, String name,
            double lat, double lon, String photoPath, String description,
            String createdByUid, String createdAt, List<BlockLineDto> lines
    ) {}

    public record BlockLineDto(
            String id, String name, String grade, String startType,
            String linePath, int sortOrder
    ) {}

    private final SchoolBlockRepository blockRepository;
    private final SchoolRepository schoolRepository;

    public SchoolBlockUseCase(SchoolBlockRepository blockRepository, SchoolRepository schoolRepository) {
        this.blockRepository = blockRepository;
        this.schoolRepository = schoolRepository;
    }

    public List<BlockDto> listBySchool(String schoolId) {
        return blockRepository.findBySchoolId(schoolId).stream().map(this::toDto).toList();
    }

    @Transactional
    public BlockDto create(String creatorUid, String schoolId, CreateBlockRequest req) {
        schoolRepository.findById(schoolId)
                .orElseThrow(() -> new SchoolNotFoundException(schoolId));

        if (req.name() == null || req.name().isBlank())
            throw new IllegalArgumentException("name required");
        if (req.lat() == null || req.lon() == null)
            throw new IllegalArgumentException("lat/lon required");
        SchoolBlock.Type type = SchoolBlock.Type.valueOf(req.type());

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
                schoolId, type, req.name().trim(),
                req.lat(), req.lon(),
                req.photoPath(),
                req.description(),
                creatorUid, LocalDateTime.now(), lines
        );
        return toDto(blockRepository.save(block));
    }

    @Transactional
    public void delete(String adminUid, String blockId) {
        SchoolBlock b = blockRepository.findById(blockId)
                .orElseThrow(() -> new SchoolNotFoundException(blockId));
        // Solo creador o admin (simplificado: solo creador)
        if (!b.getCreatedByUid().equals(adminUid))
            throw new ForbiddenException("No es tu bloque");
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
                )).toList()
        );
    }
}
