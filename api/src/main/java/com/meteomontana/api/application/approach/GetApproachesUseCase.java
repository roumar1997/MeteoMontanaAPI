package com.meteomontana.api.application.approach;

import com.meteomontana.api.domain.model.Approach;
import com.meteomontana.api.domain.model.ApproachPin;
import com.meteomontana.api.domain.port.ApproachRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lee las aproximaciones (caminos) de una escuela con sus chinchetas. Fase 1
 * de APPROACH_DESIGN.md — solo lectura, sin alta por usuario todavía.
 */
@Service
@RequiredArgsConstructor
public class GetApproachesUseCase {

    public record ApproachPinDto(
            String id, double lat, double lon, int positionIdx,
            String kind, String message, String photoPath,
            String authorUid, String status
    ) {}

    public record ApproachDto(
            String id, String schoolId, String fromBlockId, String toBlockId,
            String name, String pathJson, Integer distanceM, Integer ascentM,
            Integer durationMin, String source, String status, String authorUid,
            List<ApproachPinDto> pins
    ) {}

    private final ApproachRepository repository;

    @Transactional(readOnly = true)
    public List<ApproachDto> listBySchool(String schoolId) {
        return repository.findBySchoolId(schoolId).stream().map(this::toDto).toList();
    }

    private ApproachDto toDto(Approach a) {
        List<ApproachPinDto> pins = a.getPins().stream().map(this::toDto).toList();
        return new ApproachDto(
                a.getId(), a.getSchoolId(), a.getFromBlockId(), a.getToBlockId(),
                a.getName(), a.getPathJson(), a.getDistanceM(), a.getAscentM(),
                a.getDurationMin(), a.getSource().name(), a.getStatus().name(),
                a.getAuthorUid(), pins);
    }

    private ApproachPinDto toDto(ApproachPin p) {
        return new ApproachPinDto(
                p.getId(), p.getLat(), p.getLon(), p.getPositionIdx(),
                p.getKind().name(), p.getMessage(), p.getPhotoPath(),
                p.getAuthorUid(), p.getStatus().name());
    }
}
