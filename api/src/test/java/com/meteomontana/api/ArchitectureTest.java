package com.meteomontana.api;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * GUARDARRAÍL de la arquitectura hexagonal (ARCHITECTURE.md §1).
 *
 * Estos tests son la razón de que el refactor no se degrade: si una feature
 * nueva mete JPA en un caso de uso o Spring en el dominio, el CI se pone rojo
 * ANTES de que la deuda entre en main. Si una regla estorba, se discute y se
 * cambia la regla — no se ignora en silencio.
 *
 * Las excepciones vivas están listadas explícitamente con su motivo y su
 * tarea de saldo, para que se vean y no se olviden.
 */
class ArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.meteomontana.api");
    }

    /**
     * El DOMINIO es puro: sin Spring, sin JPA, sin Jackson, sin web.
     * (El dominio describe el negocio; los frameworks son detalles de fuera.)
     */
    @Test
    void elDominioNoDependeDeFrameworks() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "com.fasterxml.jackson..")
                .because("el dominio debe poder compilarse y testearse sin frameworks");
        rule.check(classes);
    }

    /** El dominio tampoco conoce la infraestructura (la flecha va hacia dentro). */
    @Test
    void elDominioNoDependeDeInfraestructura() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .because("la dependencia va infraestructura → dominio, nunca al revés");
        rule.check(classes);
    }

    /**
     * Los CASOS DE USO hablan con puertos, no con JPA.
     *
     * Excepciones vivas (deuda conocida, tarea P2.5 — se irán quitando de esta
     * lista según se saldan; el subsistema social/feed YA está limpio):
     *   alerts, contribution, moderation, note, admin, blocks, submissions,
     *   users, meetups.ResolveReport
     */
    @Test
    void losCasosDeUsoNoTocanJpaDirectamente() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..application..")
                .and().resideOutsideOfPackages(
                        // ── Deuda pendiente (P2.5). Al saldar cada subsistema,
                        //    se borra su línea de aquí y el test lo vigila para siempre.
                        "..application.alerts..",
                        "..application.contribution..",
                        "..application.moderation..",
                        "..application.blocks..",
                        "..application.users..",
                        "..application.meetups..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure.persistence.jpa..")
                .because("los casos de uso dependen de puertos de dominio, no de entidades JPA");
        rule.check(classes);
    }

    /** El mapeo excepción→HTTP vive SOLO en GlobalExceptionHandler. */
    @Test
    void elDominioNoConoceHttp() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework.web..", "jakarta.servlet..")
                .because("el dominio no sabe que existe HTTP; el mapeo vive en GlobalExceptionHandler");
        rule.check(classes);
    }
}
