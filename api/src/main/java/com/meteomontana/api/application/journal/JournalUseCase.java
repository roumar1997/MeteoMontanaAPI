package com.meteomontana.api.application.journal;

import com.meteomontana.api.domain.exception.ForbiddenException;
import com.meteomontana.api.domain.exception.SchoolNotFoundException;
import com.meteomontana.api.domain.model.JournalSession;
import com.meteomontana.api.domain.port.JournalRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class JournalUseCase {

    private final JournalRepository repository;

    public JournalUseCase(JournalRepository repository) {
        this.repository = repository;
    }

    public JournalDtos.JournalSessionDto create(String uid, JournalDtos.CreateJournalRequest req) {
        if (req.blockName() == null || req.blockName().isBlank())
            throw new IllegalArgumentException("blockName is required");
        if (req.date() == null)
            throw new IllegalArgumentException("date is required");

        JournalSession session = new JournalSession(
                UUID.randomUUID().toString(),
                uid,
                req.schoolId(), req.schoolName(),
                req.sector(),
                req.blockName().trim(),
                req.grade(),
                req.notes(),
                normalizeDiscipline(req.discipline()),
                req.lineId(),
                normalizeStatus(req.status()),
                req.date(),
                LocalDateTime.now()
        );
        return JournalDtos.JournalSessionDto.from(repository.save(session));
    }

    public List<JournalDtos.JournalSessionDto> listMine(String uid) {
        return repository.findByUid(uid).stream()
                .map(JournalDtos.JournalSessionDto::from)
                .toList();
    }

    public void delete(String uid, String id) {
        JournalSession s = repository.findById(id)
                .orElseThrow(() -> new SchoolNotFoundException(id));
        if (!s.getUid().equals(uid))
            throw new ForbiddenException("Not your session");
        repository.deleteById(id);
    }

    public JournalDtos.JournalStatsDto statsFor(String uid) {
        // Los PROYECTOS no cuentan para las stats DONE (blockCount/boulderCount/
        // routeCount/maxGrade...); solo las entradas DONE. Pero SÍ se cuentan
        // aparte (project*) en la MISMA llamada, para que "Proyectos" del perfil
        // funcione con el mismo caché offline que el resto de stats.
        List<JournalSession> everything = repository.findByUid(uid);
        List<JournalSession> all = everything.stream()
                .filter(s -> !"PROJECT".equalsIgnoreCase(s.getStatus()))
                .toList();

        int projectBoulderCount = 0, projectRouteCount = 0;
        for (JournalSession s : everything) {
            if ("PROJECT".equalsIgnoreCase(s.getStatus())) {
                if ("ROUTE".equalsIgnoreCase(s.getDiscipline())) projectRouteCount++;
                else projectBoulderCount++;
            }
        }
        int projectCount = projectBoulderCount + projectRouteCount;

        int blockCount = all.size();
        int routeCount = 0;
        Map<String, List<JournalSession>> bySchool = new LinkedHashMap<>();
        String maxGrade = null, maxBoulderGrade = null, maxRouteGrade = null;
        for (JournalSession s : all) {
            String key = s.getSchoolName() != null ? s.getSchoolName() : "(sin escuela)";
            bySchool.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
            maxGrade = JournalGradeRank.max(maxGrade, s.getGrade());
            if ("ROUTE".equalsIgnoreCase(s.getDiscipline())) {
                routeCount++;
                maxRouteGrade = JournalGradeRank.max(maxRouteGrade, s.getGrade());
            } else {
                maxBoulderGrade = JournalGradeRank.max(maxBoulderGrade, s.getGrade());
            }
        }
        int boulderCount = blockCount - routeCount;

        List<JournalDtos.JournalStatsDto.SchoolStats> perSchool = new ArrayList<>();
        for (var entry : bySchool.entrySet()) {
            String schoolMax = null;
            for (JournalSession s : entry.getValue()) {
                schoolMax = JournalGradeRank.max(schoolMax, s.getGrade());
            }
            perSchool.add(new JournalDtos.JournalStatsDto.SchoolStats(
                    entry.getKey(), entry.getValue().size(), schoolMax
            ));
        }

        return new JournalDtos.JournalStatsDto(
                blockCount, boulderCount, routeCount, bySchool.size(),
                maxGrade, maxBoulderGrade, maxRouteGrade, perSchool,
                projectCount, projectBoulderCount, projectRouteCount
        );
    }

    /** Normaliza la modalidad a BOULDER/ROUTE; default BOULDER. */
    private static String normalizeDiscipline(String raw) {
        return "ROUTE".equalsIgnoreCase(raw != null ? raw.trim() : null) ? "ROUTE" : "BOULDER";
    }

    /** Normaliza el estado a DONE/PROJECT; default DONE (compat clientes antiguos). */
    private static String normalizeStatus(String raw) {
        return "PROJECT".equalsIgnoreCase(raw != null ? raw.trim() : null) ? "PROJECT" : "DONE";
    }
}
