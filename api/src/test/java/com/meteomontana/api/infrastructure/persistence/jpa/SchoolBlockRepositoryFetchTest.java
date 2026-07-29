package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.BlockLine;
import com.meteomontana.api.domain.model.SchoolBlock;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que traer los bloques de una escuela con sus líneas NO produce N+1:
 * debe resolverse en UNA sola sentencia SQL (LEFT JOIN FETCH), no en 1 + N.
 * BD en memoria (H2) — no toca producción ni necesita Docker.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.datasource.url=jdbc:h2:mem:blocks;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=DAY",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class SchoolBlockRepositoryFetchTest {

    @Autowired SpringDataSchoolBlockRepository repo;
    @Autowired EntityManager em;

    @Test
    void traeBloquesConSusLineasEnUnaSolaConsulta() {
        // 3 piedras, cada una con 3 vías, en la misma escuela.
        for (int b = 0; b < 3; b++) {
            var block = new SchoolBlockJpaEntity(
                    "block-" + b, "school-1", SchoolBlock.Type.BLOCK, String.valueOf(b + 1),
                    40.0, -3.0, "photo-" + b + ".jpg", null, "uid-1",
                    LocalDateTime.now().plusSeconds(b));
            for (int l = 0; l < 3; l++) {
                block.addLine(new BlockLineJpaEntity(
                        "line-" + b + "-" + l, "via" + l, "6a",
                        BlockLine.StartType.SIT, "[]", l, "photo-" + b + ".jpg", 0));
            }
            repo.save(block);
        }
        em.flush();
        em.clear(); // vacía el contexto para forzar lectura real de BD

        Statistics stats = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        var blocks = repo.findBySchoolIdOrderByCreatedAtAsc("school-1");
        // Toca las líneas para que, si fueran LAZY sin fetch, dispararan más queries.
        int totalLineas = blocks.stream().mapToInt(b -> b.getLines().size()).sum();
        long sentencias = stats.getPrepareStatementCount();

        assertThat(blocks).hasSize(3);                 // sin duplicados por el join
        assertThat(totalLineas).isEqualTo(9);          // 3 piedras x 3 vías cargadas
        assertThat(sentencias)
                .as("Debe ser 1 consulta (JOIN FETCH); con N+1 serían 1 + 3 = 4")
                .isEqualTo(1);
    }
}
