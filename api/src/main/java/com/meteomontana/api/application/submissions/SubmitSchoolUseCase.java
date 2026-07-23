package com.meteomontana.api.application.submissions;

import com.meteomontana.api.domain.model.SchoolSubmission;
import com.meteomontana.api.domain.model.SubmissionStatus;
import com.meteomontana.api.domain.port.PushSender;
import com.meteomontana.api.domain.port.SchoolSubmissionRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataUserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class SubmitSchoolUseCase {

    private final SchoolSubmissionRepository repository;
    private final SpringDataUserRepository userRepository;
    private final PushSender push;

    public SubmitSchoolUseCase(SchoolSubmissionRepository repository,
                               SpringDataUserRepository userRepository,
                               PushSender push) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.push = push;
    }

    public SubmissionDto execute(String submitterUid, SubmitSchoolRequest req) {
        if (req.name() == null || req.name().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (req.lat() == null || req.lon() == null) {
            throw new IllegalArgumentException("lat and lon are required");
        }
        if (req.lat() < -90 || req.lat() > 90 || req.lon() < -180 || req.lon() > 180) {
            throw new IllegalArgumentException("Invalid coordinates");
        }

        SchoolSubmission s = new SchoolSubmission(
                UUID.randomUUID().toString(),
                req.name().trim(),
                req.region(),
                req.style(),
                req.rockType(),
                req.lat(),
                req.lon(),
                req.location(),
                req.source(),
                req.notes(),
                SubmissionStatus.PENDING,
                submitterUid,
                null, null, null,
                LocalDateTime.now(),
                null
        );
        SubmissionDto saved = SubmissionDto.from(repository.save(s));
        // Avisa a los admins: escuela nueva pendiente de revisar. El target
        // `admin_contributions` abre la pestaña PROPUESTAS (donde salen las
        // escuelas nuevas y las mejoras).
        String schoolName = req.name().trim();
        userRepository.findByIsAdminTrue().forEach(admin ->
                push.sendToUser(admin.getUid(), "Escuela nueva propuesta",
                        "«" + schoolName + "» · toca para revisarla en el panel",
                        Map.of("targetType", "admin_contributions", "targetId", "")));
        return saved;
    }
}
