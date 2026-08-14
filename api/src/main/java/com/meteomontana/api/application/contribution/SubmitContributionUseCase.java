package com.meteomontana.api.application.contribution;

import com.meteomontana.api.domain.exception.BadRequestException;
import com.meteomontana.api.domain.exception.SchoolNotFoundException;
import com.meteomontana.api.domain.model.PendingContribution;
import com.meteomontana.api.domain.model.SubmissionStatus;
import com.meteomontana.api.domain.port.PushSender;
import com.meteomontana.api.domain.port.SchoolRepository;
import com.meteomontana.api.domain.port.PendingContributionRepository;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubmitContributionUseCase {

    private final PendingContributionRepository repo;
    private final SchoolRepository schoolRepository;
    private final com.meteomontana.api.application.admin.AdminGuard adminGuard;
    private final ReviewContributionUseCase reviewUseCase;
    private final com.meteomontana.api.domain.port.UserRepository userRepository;
    private final PushSender push;

    public ContributionResponse execute(String schoolId, ContributionRequest req,
                                        FirebaseUser user) {
        var school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new SchoolNotFoundException(schoolId));

        PendingContribution.Type type;
        try {
            type = PendingContribution.Type.valueOf(req.type().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    "Tipo de contribución inválido: " + req.type());
        }

        var contribution = new PendingContribution(
                UUID.randomUUID().toString(), type, SubmissionStatus.PENDING,
                school.getId(), school.getName(),
                req.name(), req.lat(), req.lon(),
                req.notes(), req.description(),
                req.proposedLat(), req.proposedLon(), req.correctionReason(),
                req.targetBlockId(), req.targetLineId(), req.sectorBlockId(),
                req.photoUrl(), req.bloquesJson(), req.topoLinesJson(), req.discipline(),
                req.geometry(), req.path(), req.direction(),
                user.uid(), user.name(),
                null, null,
                LocalDateTime.now(), null
        );

        contribution.setOrientationsJson(req.orientationsJson());
        repo.save(contribution);

        // Si quien propone es ADMIN, se publica directamente (sin cola de
        // revisión): materializamos al instante reutilizando la aprobación.
        // Cubre crear piedra/parking/sector y corregir posición (incl. la
        // escuela) en ambas apps, sin cambios de UI.
        if (adminGuard.isAdmin(user.uid())) {
            // notify=false: no avisar por email al propio admin de su creación.
            return reviewUseCase.approve(contribution.getId(), user, false);
        }
        // Va a la cola de revisión: avisa a los admins por push (para saber que
        // tienen algo que revisar). Mismo patrón que las denuncias.
        notifyAdmins(school.getName(), type);
        return ContributionResponse.from(contribution);
    }

    /** Push a todos los admins: propuesta nueva pendiente de revisar. El target
     *  `admin_contributions` abre la pestaña de PROPUESTAS del panel de admin. */
    private void notifyAdmins(String schoolName, PendingContribution.Type type) {
        String what = switch (type) {
            case PARKING -> "parking";
            case SECTOR -> "sector";
            case ASSIGN_SECTOR -> "cambio de sector";
            case POSITION_CORRECTION -> "corrección de posición";
            case SCHOOL_NAME_CORRECTION -> "corrección de nombre de escuela";
            default -> "piedra/vía";
        };
        String title = "Propuesta nueva: " + what;
        String body = "En «" + schoolName + "» · toca para revisarla en el panel";
        userRepository.findAdmins().forEach(admin ->
                push.sendToUser(admin.getUid(), title, body,
                        Map.of("targetType", "admin_contributions", "targetId", "")));
    }
}
