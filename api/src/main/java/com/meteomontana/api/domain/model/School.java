package com.meteomontana.api.domain.model;

public class School {
    private final String id;
    private final String name;
    private final String location;
    private final String region;
    private final String style;
    private final String rockType;
    private final double lat;
    private final double lon;
    private final String source;

    public School(String id, String name, String location, String region,
                  String style, String rockType, double lat, double lon, String source) {
        this.id       = id;
        this.name     = name;
        this.location = location;
        this.region   = region;
        this.style    = style;
        this.rockType = rockType;
        this.lat      = lat;
        this.lon      = lon;
        this.source   = source;
    }

    public String getId()       { return id; }
    public String getName()     { return name; }
    public String getLocation() { return location; }
    public String getRegion()   { return region; }
    public String getStyle()    { return style; }
    public String getRockType() { return rockType; }
    public double getLat()      { return lat; }
    public double getLon()      { return lon; }
    public String getSource()   { return source; }
}
