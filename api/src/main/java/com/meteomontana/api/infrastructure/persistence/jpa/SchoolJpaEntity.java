package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "schools")
public class SchoolJpaEntity {

    @Id
    private  String id;
    @Column(nullable = false)
    private String name;
    private String location;
    private String region;
    private String style;

    @Column( name = "rock_type")
    private String rockType;

    @Column( nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lon;


    private String source;

    protected SchoolJpaEntity(){}

    public SchoolJpaEntity(String id, String name, String location, String region, String style,
                           String rockType, double lat, double lon, String source){

        this.id = id;
        this.name =  name;
        this.location = location;
        this.region =  region;
        this.style =  style;
        this.rockType = rockType;
        this.lat = lat;
        this.lon = lon;
        this.source = source;
    }

    public String getId() {return id; }
    public String getName() {return name; }
    public String getLocation() {return location; }
    public String getRegion() {return region; }
    public String getStyle() {return style; }
    public String getRockType() {return rockType; }
    public double getLat() {return  lat; }
    public double getLon() {return lon; }
    public String getSource() {return source; }

}
