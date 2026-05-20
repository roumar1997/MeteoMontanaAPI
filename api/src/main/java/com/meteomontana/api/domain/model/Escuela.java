package com.meteomontana.api.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Escuela {
     private final String id;
     private final String nombre;
     private final String ubicacion;
     private final String ccaa;
     private final String estilo;
     private final String roca;
     private final double lat;
     private final double lon;
     private final String fuente;

     @JsonCreator
     public Escuela(@JsonProperty("id")        String id,
                    @JsonProperty("nombre")    String nombre,
                    @JsonProperty("ubicacion") String ubicacion,
                    @JsonProperty("ccaa")      String ccaa,
                    @JsonProperty("estilo")    String estilo,
                    @JsonProperty("roca")      String roca,
                    @JsonProperty("lat")       double lat,
                    @JsonProperty("lon")       double lon,
                    @JsonProperty("fuente")    String fuente){
         this.id = id;
         this.nombre = nombre;
         this.ubicacion = ubicacion;
         this.ccaa = ccaa;
         this.estilo = estilo;
         this.roca = roca;
         this.lat = lat;
         this.lon = lon;
         this.fuente = fuente;

     }
    public String getId()        { return id; }
    public String getNombre()    { return nombre; }
    public String getUbicacion() { return ubicacion; }
    public String getCcaa()      { return ccaa; }
    public String getEstilo()    { return estilo; }
    public String getRoca()      { return roca; }
    public double getLat()       { return lat; }
    public double getLon()       { return lon; }
    public String getFuente()    { return fuente; }



}
