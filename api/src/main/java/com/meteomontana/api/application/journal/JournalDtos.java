package com.meteomontana.api.application.journal;

import com.meteomontana.api.domain.model.JournalSession;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class JournalDtos {

    public record CreateJournalRequest(
            String schoolId,
            String schoolName,
            String sector,
            String blockName,
            String grade,
            String notes,
            LocalDate date
    ) {}

    public record JournalSessionDto(
            String id,
            String schoolId,
            String schoolName,
            String sector,
            String blockName,
            String grade,
            String notes,
            LocalDate date,
            LocalDateTime createdAt
    ) {
        public static JournalSessionDto from(JournalSession s) {
            return new JournalSessionDto(
                    s.getId(), s.getSchoolId(), s.getSchoolName(),
                    s.getSector(), s.getBlockName(), s.getGrade(), s.getNotes(),
                    s.getSessionDate(), s.getCreatedAt()
            );
        }
    }

    /** Stats agregadas: nº bloques, nº escuelas distintas, grado máx, por escuela. */
    public record JournalStatsDto(
            int blockCount,
            int schoolCount,
            String maxGrade,
            List<SchoolStats> bySchool
    ) {
        public record SchoolStats(String schoolName, int blockCount, String maxGrade) {}
    }

    private JournalDtos() {}
}
