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
            String discipline,   // BOULDER (bloque) / ROUTE (vía); default BOULDER si null
            String lineId,       // id estable de la vía (null si offline/antiguo → match por nombre)
            String status,       // DONE | PROJECT; default DONE si null (compat con clientes antiguos)
            Boolean aVista,      // estilo de ascensión, independientes entre sí; default false si null
            Boolean alFlash
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
            String discipline,
            String lineId,
            String status,
            boolean aVista,
            boolean alFlash
    ) {
        public static JournalSessionDto from(JournalSession s) {
            return new JournalSessionDto(
                    s.getId(), s.getSchoolId(), s.getSchoolName(),
                    s.getSector(), s.getBlockName(), s.getGrade(), s.getNotes(),
                    s.getSessionDate(), s.getCreatedAt(),
                    s.getDiscipline() != null ? s.getDiscipline() : "BOULDER",
                    s.getLineId(),
                    s.getStatus() != null ? s.getStatus() : "DONE",
                    s.isAVista(), s.isAlFlash()
            );
        }
    }

    /** Stats agregadas: nº bloques (BOULDER), nº vías (ROUTE), nº escuelas,
     *  grado máx y desglose por escuela. blockCount = boulderCount + routeCount
     *  (total) por compatibilidad con clientes antiguos. project* cuenta las
     *  entradas PROJECT (probando, aún no hecho) — separado del resto, que solo
     *  cuenta DONE. Viene en la MISMA llamada que el resto de stats para que
     *  "Proyectos" funcione offline igual que Bloques/Vías/Escuelas (se cachea
     *  junto con todo lo demás, sin round-trip aparte). */
    public record JournalStatsDto(
            int blockCount,
            int boulderCount,
            int routeCount,
            int schoolCount,
            String maxGrade,         // grado máx global (cualquier modalidad), por compat
            String maxBoulderGrade,  // grado máx de bloque
            String maxRouteGrade,    // grado máx de vía
            List<SchoolStats> bySchool,
            int projectCount,
            int projectBoulderCount,
            int projectRouteCount
    ) {
        public record SchoolStats(String schoolName, int blockCount, String maxGrade) {}
    }

    /** Cambiar el estilo (a vista / al flash) de una entrada. Independientes entre sí. */
    public record UpdateStyleRequest(boolean aVista, boolean alFlash) {}

    private JournalDtos() {}
}
