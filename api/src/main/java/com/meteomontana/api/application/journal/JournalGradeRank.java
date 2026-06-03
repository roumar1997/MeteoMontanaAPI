package com.meteomontana.api.application.journal;

import java.util.List;

/**
 * Compara grados de escalada (sistema francés/europeo).
 * 5a < 5b < ... < 7a < 7a+ < 7b < 7b+ < 7c < 7c+ < 8a ...
 */
public final class JournalGradeRank {

    private static final List<String> ORDER = List.of(
            "3", "4", "4+", "5a", "5b", "5c",
            "6a", "6a+", "6b", "6b+", "6c", "6c+",
            "7a", "7a+", "7b", "7b+", "7c", "7c+",
            "8a", "8a+", "8b", "8b+", "8c", "8c+",
            "9a", "9a+", "9b", "9b+", "9c"
    );

    public static int rank(String grade) {
        if (grade == null) return -1;
        int i = ORDER.indexOf(grade.toLowerCase().trim());
        return i;
    }

    public static String max(String a, String b) {
        if (a == null) return b;
        if (b == null) return a;
        return rank(a) >= rank(b) ? a : b;
    }

    private JournalGradeRank() {}
}
