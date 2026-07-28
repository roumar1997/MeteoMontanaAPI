package com.meteomontana.api.domain.score;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Regla del GRADO POR CONSENSO (dominio puro, decisión de Rodrigo 2026-07-29):
 * hasta {@link #MIN_VOTES_FOR_CONSENSUS - 1} votos se muestra el grado del
 * equipador; con 3+ manda la mayoría — y solo la mayoría. Empate: gana el
 * grado MÁS DURO no, ni el más blando: el que ya se estaba mostrando si está
 * entre los empatados; si no, el más votado más antiguo (orden alfabético
 * estable para que sea determinista).
 */
public final class GradeConsensus {

    public static final int MIN_VOTES_FOR_CONSENSUS = 3;

    private GradeConsensus() {}

    /**
     * Grado a mostrar dadas los votos ("6c" → 9, "6c+" → 5...), el grado del
     * equipador y el mostrado actual (para desempates estables).
     */
    public static String displayedGrade(Map<String, Integer> votes,
                                        String setterGrade, String currentDisplayed) {
        int total = votes.values().stream().mapToInt(Integer::intValue).sum();
        if (total < MIN_VOTES_FOR_CONSENSUS) return setterGrade;

        int max = votes.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<String> top = votes.entrySet().stream()
                .filter(e -> e.getValue() == max)
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());
        if (top.contains(currentDisplayed)) return currentDisplayed;
        return top.stream().min(Comparator.naturalOrder()).orElse(setterGrade);
    }

    /** Consenso de orientación: la mayoría simple (1 voto ya decide); null sin votos. */
    public static String orientationConsensus(Map<String, Integer> votes) {
        return votes.entrySet().stream()
                .max(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                        .thenComparing(Map.Entry::getKey, Comparator.reverseOrder()))
                .map(Map.Entry::getKey).orElse(null);
    }
}
