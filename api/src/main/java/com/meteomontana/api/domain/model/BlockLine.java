package com.meteomontana.api.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
public class BlockLine {
    // SEMI = semi-sentado/incorporado (añadido 2026-07-18; la columna es
    // VARCHAR vía @Enumerated(STRING) → sin migración; apps viejas tratan
    // startType como String y muestran el valor sin etiqueta, no rompen).
    public enum StartType { SIT, SEMI, STAND, JUMP, TRAV }

    private final String id;
    private final String blockId;
    private final String name;
    private final String grade;
    private final StartType startType;
    private final String linePath;     // JSON con array de puntos
    private final int sortOrder;
    private final String photoPath;    // foto (cara) sobre la que está dibujada esta vía
    private final int faceOrder;       // orden de su cara dentro de la piedra
    // Beta/detalle opcional. Mutable (setter) para no tocar los 8 constructores
    // repartidos por el código; se rellena solo donde aplica.
    @Setter
    private String description;
    // Variante opcional ("directa", "extensión"...) — distingue vías homónimas.
    // Mismo patrón mutable que description.
    @Setter
    private String variant;
    // Grado ORIGINAL del equipador (V60): `grade` pasa a ser el MOSTRADO
    // (consenso comunitario con 3+ votos). Mismo patrón mutable.
    @Setter
    private String setterGrade;

    public BlockLine(String id, String blockId, String name, String grade,
                     StartType startType, String linePath, int sortOrder) {
        this(id, blockId, name, grade, startType, linePath, sortOrder, null, 0);
    }

    public BlockLine(String id, String blockId, String name, String grade,
                     StartType startType, String linePath, int sortOrder,
                     String photoPath, int faceOrder) {
        this.id = id; this.blockId = blockId; this.name = name;
        this.grade = grade; this.startType = startType;
        this.linePath = linePath; this.sortOrder = sortOrder;
        this.photoPath = photoPath; this.faceOrder = faceOrder;
    }

}
