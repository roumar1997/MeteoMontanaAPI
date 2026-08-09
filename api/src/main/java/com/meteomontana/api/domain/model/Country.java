package com.meteomontana.api.domain.model;

import java.util.List;

/**
 * Un país en el que hay (o puede haber) escuelas, con sus regiones.
 *
 * <p>Existe porque hasta ahora la lista de regiones se deducía de las escuelas
 * que ya había en la base de datos. Eso funciona mientras solo exista España
 * con 191 escuelas, pero el día que se abre un país nuevo el desplegable sale
 * VACÍO y el primero que quiera proponer una escuela allí no puede elegir
 * región. Con el catálogo servido, un país empieza a existir aunque no tenga
 * todavía ninguna escuela.
 *
 * <p>{@code code} es ISO 3166-1 alfa-2 en mayúsculas (ES, FR, PT). Es lo que se
 * guarda en la base de datos y lo que deciden las apps para saber si aplican
 * los servicios de AEMET, que son solo de España.
 */
public record Country(String code, String name, List<String> regions) {
}
