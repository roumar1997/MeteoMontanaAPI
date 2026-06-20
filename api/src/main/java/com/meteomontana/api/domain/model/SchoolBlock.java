package com.meteomontana.api.domain.model;

import java.time.LocalDateTime;
import java.util.List;

public class SchoolBlock {
    public enum Type { BLOCK, PARKING, ZONE }

    /** Modalidad de escalada de la piedra: bloque (BOULDER) o vía (ROUTE).
     *  Solo aplica a las piedras (type=BLOCK); para PARKING/ZONE se ignora. */
    public enum Discipline { BOULDER, ROUTE }

    private final String id;
    private final String schoolId;
    private final Type type;
    private final Discipline discipline;
    private final String name;
    private final double lat;
    private final double lon;
    private final String photoPath;
    private final String description;
    private final String createdByUid;
    private final LocalDateTime createdAt;
    private final List<BlockLine> lines;
    private final String sectorBlockId;   // null si la piedra no está asignada a un sector

    public SchoolBlock(String id, String schoolId, Type type, Discipline discipline, String name,
                       double lat, double lon, String photoPath, String description,
                       String createdByUid, LocalDateTime createdAt, List<BlockLine> lines,
                       String sectorBlockId) {
        this.id = id; this.schoolId = schoolId; this.type = type;
        this.discipline = discipline != null ? discipline : Discipline.BOULDER;
        this.name = name;
        this.lat = lat; this.lon = lon; this.photoPath = photoPath;
        this.description = description; this.createdByUid = createdByUid;
        this.createdAt = createdAt; this.lines = lines; this.sectorBlockId = sectorBlockId;
    }

    public SchoolBlock(String id, String schoolId, Type type, String name,
                       double lat, double lon, String photoPath, String description,
                       String createdByUid, LocalDateTime createdAt, List<BlockLine> lines,
                       String sectorBlockId) {
        this(id, schoolId, type, Discipline.BOULDER, name, lat, lon, photoPath, description,
             createdByUid, createdAt, lines, sectorBlockId);
    }

    public SchoolBlock(String id, String schoolId, Type type, String name,
                       double lat, double lon, String photoPath, String description,
                       String createdByUid, LocalDateTime createdAt, List<BlockLine> lines) {
        this(id, schoolId, type, name, lat, lon, photoPath, description,
             createdByUid, createdAt, lines, null);
    }

    public String getId()             { return id; }
    public String getSchoolId()       { return schoolId; }
    public Type getType()             { return type; }
    public Discipline getDiscipline() { return discipline; }
    public String getName()           { return name; }
    public double getLat()            { return lat; }
    public double getLon()            { return lon; }
    public String getPhotoPath()      { return photoPath; }
    public String getDescription()    { return description; }
    public String getCreatedByUid()   { return createdByUid; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
    public List<BlockLine> getLines() { return lines; }
    public String getSectorBlockId()  { return sectorBlockId; }
}
