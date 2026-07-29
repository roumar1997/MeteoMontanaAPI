package com.meteomontana.api.domain.model;

import java.util.List;
import java.util.Map;

/** Modelos de dominio de los votos comunitarios (orientación y grado). */
public final class CommunityVotes {

    private CommunityVotes() {}

    /** Un voto de orientación (photoIndex null = la piedra/sector entero). */
    public record OrientationVote(String blockId, Integer photoIndex,
                                  String voterUid, String aspect) {}

    /** Resumen para la UI: recuento por rumbo, consenso (mayoría) y mi voto. */
    public record OrientationSummary(Integer photoIndex,
                                     Map<String, Integer> votes,
                                     String consensus,
                                     String myVote) {}




    /** Un voto de grado de una vía. */
    public record GradeVote(String lineId, String voterUid, String grade) {}

    /** Resumen de grados: recuento, grado del equipador, consenso y mi voto. */
    public record GradeSummary(String lineId,
                               Map<String, Integer> votes,
                               String setterGrade,
                               String displayedGrade,
                               String myVote) {}

    /** Franja horaria de sol de una pared. */
    public record SunHour(String time, boolean inSun) {}

    /** Tira de sol del día para una superficie (piedra, sector o foto de muro). */
    public record SunHours(String aspect, List<SunHour> hours) {}
}
