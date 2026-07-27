package com.meteomontana.api.domain.model;

/**
 * Un resultado del buscador global de vías/bloques.
 * {@code lineId} != null → es una vía; null → es una piedra/muro.
 * {@code photoPath} es la cara donde está dibujada la vía (o la portada de la
 * piedra) con la misma semántica que en el endpoint de bloques; {@code linePath}
 * es el trazo normalizado de la vía — juntos permiten pintar el mini-topo en
 * los resultados. {@code schoolName} lo resuelve la capa de aplicación.
 */
public record LineSearchHit(
        String schoolId,
        String schoolName,
        String blockId,
        String blockName,
        String lineId,
        String lineName,
        String grade,
        String sectorName,
        String photoPath,
        String linePath,
        String startType
) {
    public LineSearchHit withSchoolName(String name) {
        return new LineSearchHit(schoolId, name, blockId, blockName, lineId, lineName,
                grade, sectorName, photoPath, linePath, startType);
    }
}
