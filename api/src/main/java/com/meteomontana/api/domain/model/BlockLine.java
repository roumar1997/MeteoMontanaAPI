package com.meteomontana.api.domain.model;

public class BlockLine {
    public enum StartType { SIT, STAND, JUMP, TRAV }

    private final String id;
    private final String blockId;
    private final String name;
    private final String grade;
    private final StartType startType;
    private final String linePath;     // JSON con array de puntos
    private final int sortOrder;

    public BlockLine(String id, String blockId, String name, String grade,
                     StartType startType, String linePath, int sortOrder) {
        this.id = id; this.blockId = blockId; this.name = name;
        this.grade = grade; this.startType = startType;
        this.linePath = linePath; this.sortOrder = sortOrder;
    }

    public String getId()           { return id; }
    public String getBlockId()      { return blockId; }
    public String getName()         { return name; }
    public String getGrade()        { return grade; }
    public StartType getStartType() { return startType; }
    public String getLinePath()     { return linePath; }
    public int getSortOrder()       { return sortOrder; }
}
