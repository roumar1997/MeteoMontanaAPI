package com.meteomontana.api.domain.score;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Las reglas que decidió Rodrigo (2026-07-29), fijadas en tests. */
class GradeConsensusTest {

    @Test
    void hastaDosVotosMandaElEquipador() {
        assertThat(GradeConsensus.displayedGrade(Map.of(), "6c+", "6c+")).isEqualTo("6c+");
        assertThat(GradeConsensus.displayedGrade(Map.of("6b", 2), "6c+", "6c+")).isEqualTo("6c+");
    }

    @Test
    void conTresVotosMandaLaMayoriaYSoloLaMayoria() {
        assertThat(GradeConsensus.displayedGrade(Map.of("6c", 3), "6c+", "6c+")).isEqualTo("6c");
        assertThat(GradeConsensus.displayedGrade(Map.of("6c", 9, "6c+", 5, "6b+", 1), "6c+", "6c+"))
                .isEqualTo("6c");
    }

    @Test
    void unEmpateNoBailaElGradoMostrado() {
        // 3-3 entre 6c y 6c+, y el mostrado actual es 6c+ → se queda 6c+.
        assertThat(GradeConsensus.displayedGrade(Map.of("6c", 3, "6c+", 3), "6c+", "6c+"))
                .isEqualTo("6c+");
        // Si el mostrado no está entre los empatados, resolución determinista.
        assertThat(GradeConsensus.displayedGrade(Map.of("6c", 3, "7a", 3), "6b", "6b"))
                .isEqualTo("6c");
    }

    @Test
    void consensoDeOrientacionEsMayoriaSimpleDesdeElPrimerVoto() {
        assertThat(GradeConsensus.orientationConsensus(Map.of("SO", 1))).isEqualTo("SO");
        assertThat(GradeConsensus.orientationConsensus(Map.of("SO", 12, "O", 3, "S", 1)))
                .isEqualTo("SO");
        assertThat(GradeConsensus.orientationConsensus(Map.of())).isNull();
    }
}
