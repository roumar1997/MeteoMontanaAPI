package com.meteomontana.api.domain.model;

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
    private String description;

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

    public String getId()           { return id; }
    public String getBlockId()      { return blockId; }
    public String getName()         { return name; }
    public String getGrade()        { return grade; }
    public StartType getStartType() { return startType; }
    public String getLinePath()     { return linePath; }
    public int getSortOrder()       { return sortOrder; }
    public String getPhotoPath()    { return photoPath; }
    public int getFaceOrder()       { return faceOrder; }
    public String getDescription()  { return description; }
    public void setDescription(String d) { this.description = d; }
}
