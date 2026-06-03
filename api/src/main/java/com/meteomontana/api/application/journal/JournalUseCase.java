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
        List<JournalSession> all = repository.findByUid(uid);

        int blockCount = all.size();
        Map<String, List<JournalSession>> bySchool = new LinkedHashMap<>();
        String maxGrade = null;
        for (JournalSession s : all) {
            String key = s.getSchoolName() != null ? s.getSchoolName() : "(sin escuela)";
            bySchool.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
            maxGrade = JournalGradeRank.max(maxGrade, s.getGrade());
        }

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
                blockCount, bySchool.size(), maxGrade, perSchool
        );
    }
}
