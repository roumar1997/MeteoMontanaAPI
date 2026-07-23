package com.meteomontana.api.infrastructure.persistence;

import com.meteomontana.api.domain.model.BlockLine;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guarda contra la deriva enum ↔ constraint de BD: cada valor de
 * {@link BlockLine.StartType} que la app escribe en block_lines.start_type DEBE
 * estar permitido por el CHECK `chk_start_type` de las migraciones Flyway.
 *
 * Por qué existe: SEMI se añadió al enum en la 2.19.0 creyendo que una columna
 * VARCHAR "no necesita migración" — pero el CHECK de V10 solo permitía
 * SIT/STAND/JUMP/TRAV, así que toda vía SEMI reventaba al insertar y las piedras
 * nuevas con una vía SEMI no se creaban (cazado en staging el 2026-07-20). Este
 * test —sin BD, solo leyendo las migraciones— habría fallado en rojo el mismo
 * día que se añadió SEMI. Si mañana se añade otro inicio, salta hasta que su
 * migración esté.
 */
class StartTypeConstraintTest {

    private static final Path MIGRATIONS = Path.of("src/main/resources/db/migration");
    // Extrae la lista de valores de: chk_start_type ... IN ('A', 'B', ...)
    private static final Pattern CHECK = Pattern.compile(
            "chk_start_type[\\s\\S]*?IN\\s*\\(([^)]*)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern VALUE = Pattern.compile("'([^']+)'");

    @Test
    void todoStartTypeDelEnumEstaPermitidoPorElCheckDeBd() throws IOException {
        Set<String> allowed = allowedValuesFromLatestMigration();

        Set<String> enumValues = new LinkedHashSet<>();
        for (BlockLine.StartType t : BlockLine.StartType.values()) enumValues.add(t.name());

        assertThat(allowed)
                .as("chk_start_type debe permitir TODOS los inicios del enum. "
                        + "Si añades un StartType, añade su migración que amplíe el CHECK.")
                .containsAll(enumValues);
    }

    /** Valores del ÚLTIMO chk_start_type definido en las migraciones (el vigente). */
    private Set<String> allowedValuesFromLatestMigration() throws IOException {
        String lastCheck = null;
        try (Stream<Path> files = Files.list(MIGRATIONS)) {
            for (Path f : files.sorted().toList()) {
                String sql = Files.readString(f);
                Matcher m = CHECK.matcher(sql);
                while (m.find()) lastCheck = m.group(1); // el último gana (migración más nueva)
            }
        }
        assertThat(lastCheck).as("no se encontró chk_start_type en las migraciones").isNotNull();
        Set<String> values = new LinkedHashSet<>();
        Matcher v = VALUE.matcher(lastCheck);
        while (v.find()) values.add(v.group(1));
        return values;
    }
}
