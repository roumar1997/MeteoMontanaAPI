package com.meteomontana.api.application.geo;

import com.meteomontana.api.domain.model.Country;

import java.util.List;
import java.util.Optional;

/**
 * Catálogo de países y sus regiones.
 *
 * <p>Vive en el servidor a propósito: abrir un país nuevo es entonces un
 * despliegue del backend, no una versión nueva en Play y en la App Store con
 * los días de revisión que eso arrastra.
 *
 * <p>Las regiones son las oficiales de cada país —comunidades autónomas en
 * España, régions en Francia, distritos en Portugal— y su orden es alfabético,
 * que es como se buscan en un desplegable.
 *
 * <p>ESPAÑA es el país por defecto: las 191 escuelas que ya existían son
 * españolas y así se marcaron en la migración.
 */
public final class CountryCatalog {

    /** Lo que se guarda cuando nadie dice otra cosa. */
    public static final String DEFAULT_CODE = "ES";

    private static final List<Country> COUNTRIES = List.of(
            new Country("ES", "España", List.of(
                    "Andalucía", "Aragón", "Asturias", "Baleares", "Canarias",
                    "Cantabria", "Castilla-La Mancha", "Castilla y León", "Cataluña",
                    "Ceuta", "Comunidad Valenciana", "Extremadura", "Galicia",
                    "La Rioja", "Madrid", "Melilla", "Murcia", "Navarra", "País Vasco")),
            new Country("FR", "Francia", List.of(
                    "Auvernia-Ródano-Alpes", "Borgoña-Franco Condado", "Bretaña",
                    "Centro-Valle del Loira", "Córcega", "Gran Este", "Alta Francia",
                    "Isla de Francia", "Normandía", "Nueva Aquitania", "Occitania",
                    "País del Loira", "Provenza-Alpes-Costa Azul")),
            new Country("PT", "Portugal", List.of(
                    "Aveiro", "Azores", "Beja", "Braga", "Braganza", "Castelo Branco",
                    "Coímbra", "Évora", "Faro", "Guarda", "Leiría", "Lisboa", "Madeira",
                    "Portalegre", "Oporto", "Santarém", "Setúbal", "Viana do Castelo",
                    "Vila Real", "Viseu"))
    );

    private CountryCatalog() {
    }

    public static List<Country> all() {
        return COUNTRIES;
    }

    public static Optional<Country> byCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        String normalizado = code.trim().toUpperCase();
        return COUNTRIES.stream().filter(c -> c.code().equals(normalizado)).findFirst();
    }

    /**
     * Normaliza lo que llegue a un código válido, cayendo a España.
     *
     * <p>Se prefiere caer a España antes que rechazar: una app vieja no manda
     * país, y sus propuestas son de España en la práctica.
     */
    public static String normalize(String code) {
        return byCode(code).map(Country::code).orElse(DEFAULT_CODE);
    }

    /**
     * ¿Aplican aquí los servicios de AEMET (radar y boletín de montaña)?
     *
     * <p>Son de la agencia estatal española: fuera de España no hay imagen de
     * radar ni boletín, así que las apps los esconden en vez de enseñar un
     * hueco vacío o un error.
     */
    public static boolean hasSpanishWeatherServices(String countryCode) {
        return DEFAULT_CODE.equals(normalize(countryCode));
    }
}
