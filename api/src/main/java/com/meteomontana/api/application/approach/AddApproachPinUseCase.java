package com.meteomontana.api.application.approach;

import com.meteomontana.api.application.admin.AdminGuard;
import com.meteomontana.api.domain.exception.SchoolNotFoundException;
import com.meteomontana.api.domain.model.ApproachPin;
import com.meteomontana.api.domain.port.ApproachRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Añade una chincheta a una aproximación — SOLO ADMIN por ahora (mismo motivo
 * que CreateApproachUseCase). La regla de negocio real —foto y/o texto,
 * nunca vacía— vive en el propio constructor de ApproachPin.
 */
@Service
@RequiredArgsConstructor
public class AddApproachPinUseCase {

    public record AddPinRequest(
            Double lat, Double lon, Integer positionIdx,
            String kind,     // FORK / LANDMARK / HAZARD / KEY
            String message,  // opcional (pero foto o mensaje, uno de los dos)
            String photoPath // opcional
    ) {}

    private final ApproachRepository approachRepository;
    private final AdminGuard adminGuard;

    @Transactional
    public GetApproachesUseCase.ApproachPinDto add(String adminUid, String approachId, AddPinRequest req) {
        adminGuard.ensureAdmin(adminUid);
        approachRepository.findById(approachId).orElseThrow(() -> new SchoolNotFoundException(approachId));
        if (req.lat() == null || req.lon() == null) {
            throw new IllegalArgumentException("lat/lon required");
        }
        ApproachPin pin = new ApproachPin(
                UUID.randomUUID().toString(), approachId, req.lat(), req.lon(),
                req.positionIdx() != null ? req.positionIdx() : 0,
                ApproachPin.Kind.valueOf(req.kind() != null ? req.kind() : "LANDMARK"),
                req.message(), req.photoPath(), adminUid,
                ApproachPin.Status.VERIFIED, LocalDateTime.now());
        ApproachPin saved = approachRepository.addPin(approachId, pin);
        return toDto(saved);
    }

    private GetApproachesUseCase.ApproachPinDto toDto(ApproachPin p) {
        return new GetApproachesUseCase.ApproachPinDto(
                p.getId(), p.getLat(), p.getLon(), p.getPositionIdx(),
                p.getKind().name(), p.getMessage(), p.getPhotoPath(),
                p.getAuthorUid(), p.getStatus().name());
    }
}
