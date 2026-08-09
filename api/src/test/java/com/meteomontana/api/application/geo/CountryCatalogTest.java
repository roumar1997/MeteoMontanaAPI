package com.meteomontana.api.application.geo;

import com.meteomontana.api.domain.model.Country;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Catálogo de países y regiones.
 *
 * <p>Hasta ahora las regiones se deducían de las escuelas existentes, así que
 * un país recién abierto salía con el desplegable VACÍO y nadie podía proponer
 * la primera escuela. Estos tests protegen justo eso: que cada país abierto
 * traiga sus regiones desde el primer día.
 */
class CountryCatalogTest {

    @Test
    void espanaFranciaYPortugalEstanAbiertos() {
        assertThat(CountryCatalog.all())
                .extracting(Country::code)
                .containsExactlyInAnyOrder("ES", "FR", "PT");
    }

    @Test
    void ningunPaisSeQuedaSinRegiones() {
        // Es LA razón de ser del catálogo: sin regiones, el desplegable sale
        // vacío y no se puede proponer la primera escuela del país.
        for (Country c : CountryCatalog.all()) {
            assertThat(c.regions())
                    .as("regiones de " + c.code())
                    .isNotEmpty();
        }
    }

    @Test
    void noHayRegionesRepetidasNiVacias() {
        for (Country c : CountryCatalog.all()) {
            Set<String> vistas = new HashSet<>();
            for (String r : c.regions()) {
                assertThat(r).as("región vacía en " + c.code()).isNotBlank();
                assertThat(vistas.add(r)).as("región repetida en " + c.code() + ": " + r).isTrue();
            }
        }
    }

    @Test
    void espanaTraeSusComunidades() {
        List<String> regiones = CountryCatalog.byCode("ES").orElseThrow().regions();
        assertThat(regiones).contains("Madrid", "Cataluña", "Andalucía", "País Vasco");
        assertThat(regiones).hasSize(19);   // 17 autonomías + Ceuta y Melilla
    }

    @Test
    void elCodigoSeAceptaEnMinusculaYConEspacios() {
        assertThat(CountryCatalog.byCode(" fr ").orElseThrow().name()).isEqualTo("Francia");
    }

    @Test
    void loDesconocidoCaeAEspana() {
        // Preferimos caer a España antes que rechazar: una app vieja no manda
        // país y sus propuestas son españolas en la práctica.
        assertThat(CountryCatalog.normalize(null)).isEqualTo("ES");
        assertThat(CountryCatalog.normalize("")).isEqualTo("ES");
        assertThat(CountryCatalog.normalize("XX")).isEqualTo("ES");
        assertThat(CountryCatalog.normalize("de")).isEqualTo("ES");
    }

    @Test
    void losServiciosDeAemetSonSoloDeEspana() {
        // Radar y boletín de montaña son de la agencia estatal española: fuera
        // no hay imagen ni boletín, y las apps deben esconderlos.
        assertThat(CountryCatalog.hasSpanishWeatherServices("ES")).isTrue();
        assertThat(CountryCatalog.hasSpanishWeatherServices(null)).isTrue();   // app vieja
        assertThat(CountryCatalog.hasSpanishWeatherServices("FR")).isFalse();
        assertThat(CountryCatalog.hasSpanishWeatherServices("PT")).isFalse();
    }
}
