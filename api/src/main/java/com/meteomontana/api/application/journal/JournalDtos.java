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
            LocalDate date,
            String discipline    // BOULDER (bloque) / ROUTE (vía); default BOULDER si null
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
            LocalDateTime createdAt,
            String discipline
    ) {
        public static JournalSessionDto from(JournalSession s) {
            return new JournalSessionDto(
                    s.getId(), s.getSchoolId(), s.getSchoolName(),
                    s.getSector(), s.getBlockName(), s.getGrade(), s.getNotes(),
                    s.getSessionDate(), s.getCreatedAt(),
                    s.getDiscipline() != null ? s.getDiscipline() : "BOULDER"
            );
        }
    }

    /** Stats agregadas: nº bloques (BOULDER), nº vías (ROUTE), nº escuelas,
     *  grado máx y desglose por escuela. blockCount = boulderCount + routeCount
     *  (total) por compatibilidad con clientes antiguos. */
    public record JournalStatsDto(
            int blockCount,
            int boulderCount,
            int routeCount,
            int schoolCount,
            String maxGrade,
            List<SchoolStats> bySchool
    ) {
        public record SchoolStats(String schoolName, int blockCount, String maxGrade) {}
    }

    private JournalDtos() {}
}
