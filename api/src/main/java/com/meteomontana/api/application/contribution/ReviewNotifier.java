package com.meteomontana.api.application.contribution;

import com.meteomontana.api.domain.model.PendingContribution;
import com.meteomontana.api.domain.port.UserRepository;
import com.meteomontana.api.infrastructure.email.EmailTemplates;
import com.meteomontana.api.infrastructure.email.ResendEmailService;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

/**
 * Email al autor de una contribución cuando se aprueba/rechaza. Best-effort:
 * un fallo construyendo o enviando NUNCA tumba la revisión (corre dentro de la
 * transacción del approve/reject). Extraído de ReviewContributionUseCase (SRP).
 */
@Service
@RequiredArgsConstructor
public class ReviewNotifier {

    private static final org.slf4j.Logger log =
        org.slf4j.LoggerFactory.getLogger(ReviewNotifier.class);

    private final ResendEmailService emailService;
    private final UserRepository userRepository;

    public void sendReviewEmail(PendingContribution c, boolean approved, String reason) {
        try {
            doSendReviewEmail(c, approved, reason);
        } catch (Exception e) {
            log.warn("Email de revisión (approved={}) de {} FALLÓ: {}",
                approved, c.getId(), e.toString());
        }
    }

    private void doSendReviewEmail(PendingContribution c, boolean approved, String reason) {
        var user = userRepository.findByUid(c.getSubmittedByUid()).orElse(null);
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            log.info("Email de revisión (approved={}) de {} OMITIDO: autor {} sin email en BD",
                approved, c.getId(), c.getSubmittedByUid());
            return;
        }
        String typeLabel = switch (c.getType()) {
            case PARKING -> "parking"; case BOULDER -> "piedra";
            case SECTOR -> "sector"; case POSITION_CORRECTION -> "corrección de posición";
            case ASSIGN_SECTOR -> "asignación de sector";
            case SCHOOL_NAME_CORRECTION -> "corrección de nombre de escuela";
        };

        // "parking de El Escorial · Parking principal"
        String proposalLabel = typeLabel + " en " + c.getSchoolName()
                + (c.getName() != null && !c.getName().isBlank() ? " · " + c.getName() : "");

        String subject;
        String inner;
        if (approved) {
            subject = "✅ Tu propuesta ya está publicada en Cumbre";
            inner = EmailTemplates.eyebrow("Propuesta aprobada")
                + EmailTemplates.title("¡Ya está en el mapa!")
                + EmailTemplates.paragraph(
                        "Hemos revisado tu propuesta y la hemos publicado. "
                        + "Desde ahora cualquier escalador puede verla en la app.")
                + EmailTemplates.highlightBox(
                        "Tu propuesta", EmailTemplates.escape(proposalLabel))
                + EmailTemplates.paragraph(
                        "Gracias por hacer crecer la guía entre todos. "
                        + "La comunidad escaladora te lo agradece. 🤘")
                // TODO: cuando la app Android esté publicada en Play Store, reactivar el botón
                // "Ver en la app" con un Android App Link tipo https://climbingteams.com/schools/{schoolId}
                // que abra la app si está instalada (requiere assetlinks.json en /.well-known/).
                + EmailTemplates.signature();
        } else {
            subject = "Tu propuesta en Cumbre no se ha podido publicar";
            inner = EmailTemplates.eyebrow("Propuesta revisada")
                + EmailTemplates.title("Esta vez no ha podido ser")
                + EmailTemplates.paragraph(
                        "Hemos revisado tu propuesta y no la hemos podido publicar.")
                + EmailTemplates.highlightBox(
                        "Tu propuesta", EmailTemplates.escape(proposalLabel))
                + (reason != null && !reason.isBlank()
                        ? EmailTemplates.highlightBox("Motivo", EmailTemplates.escape(reason))
                        : "")
                + EmailTemplates.paragraph(
                        "Si crees que es un error o quieres dar más detalles, "
                        + "puedes volver a enviarla o escribirnos desde la app.")
                + EmailTemplates.signature();
        }

        String preheader = approved
                ? "Tu " + proposalLabel + " ya está publicada."
                : "Hemos revisado tu " + proposalLabel + ".";
        boolean sent = emailService.send(user.getEmail(), subject,
                EmailTemplates.wrap(preheader, inner));
        // Rastro para diagnosticar "el email no llega": buscar "Email de revisión"
        // en los logs de Railway dice si se intentó, a quién y si Resend lo aceptó.
        log.info("Email de revisión (approved={}) de {} a {}: enviado={}",
                approved, c.getId(), user.getEmail(), sent);
    }
}
