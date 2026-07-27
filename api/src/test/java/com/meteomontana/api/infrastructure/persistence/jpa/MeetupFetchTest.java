package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MeetupJpaEntity tiene DOS colecciones EAGER (días y miembros). En Hibernate 5
 * eso hacía un PRODUCTO CARTESIANO (una quedada de 3 días y 4 miembros devolvía
 * 12 filas con objetos duplicados) — de ahí el punto 1.5 de MejorasFuturas.
 *
 * MEDIDO en Hibernate 6 (Spring Boot 3.5.x): NO ocurre. Hibernate 6 carga cada
 * colección en su propia consulta (3 sentencias: quedada + días + miembros) y
 * no duplica nada. Se comprobó con y sin {@code @Fetch(SUBSELECT)}: mismo
 * resultado y mismas 3 consultas, así que NO se añadió la anotación (habría
 * sido código que no hace nada).
 *
 * Este test se queda como GUARDARRAÍL: si alguien mete un JOIN FETCH de las dos
 * colecciones, o si un cambio de versión reintroduce el cartesiano, salta aquí.
 * BD en memoria (H2) — no toca producción ni necesita Docker.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        // NON_KEYWORDS=DAY: la columna se llama `day`, que H2 reserva y Postgres no.
        "spring.datasource.url=jdbc:h2:mem:meetups;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=DAY",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class MeetupFetchTest {

    @Autowired SpringDataMeetupRepository repo;
    @Autowired EntityManager em;

    @Test
    void lasDosColeccionesEagerNoDuplicanFilasNiDisparanNMasUno() {
        // Una quedada con 3 días y 4 miembros: el cartesiano daría 12 filas.
        var meetup = new MeetupJpaEntity(
                "m1", "school-1", "Quedada", null, "BOULDER", "OPEN", null, null,
                "uid-creator", "conv-1",
                LocalDate.now().plusDays(3), LocalDateTime.now().plusDays(4), LocalDateTime.now());
        for (int d = 1; d <= 3; d++) {
            meetup.getDays().add(new MeetupDayJpaEntity("m1", LocalDate.now().plusDays(d)));
        }
        for (int u = 0; u < 4; u++) {
            meetup.getMembers().add(
                    new MeetupMemberJpaEntity("m1", "uid-" + u, LocalDateTime.now()));
        }
        repo.save(meetup);
        em.flush();
        em.clear();   // vacía el contexto para forzar lectura real de BD

        Statistics stats = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        // La query REAL de producción (lista de quedadas activas): es donde el
        // producto cartesiano se manifestaría, no en un findById.
        var activas = repo.findActiveOrderByLastDay(LocalDateTime.now());
        assertThat(activas).as("una sola quedada, sin filas duplicadas").hasSize(1);
        var loaded = activas.get(0);

        assertThat(loaded.getDays().size())
                .as("3 días, sin duplicar por un cartesiano con miembros")
                .isEqualTo(3);
        assertThat(loaded.getMembers().size())
                .as("4 miembros, sin duplicar por un cartesiano con días")
                .isEqualTo(4);
        // 3 = quedadas + días + miembros. Si alguien reintroduce el cartesiano
        // (o un N+1 por quedada), este número se dispara.
        assertThat(stats.getPrepareStatementCount())
                .as("una consulta por colección, sin cartesiano ni N+1")
                .isLessThanOrEqualTo(3);
    }
}
