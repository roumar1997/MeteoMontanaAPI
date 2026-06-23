package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Fila de la caché persistente de forecast (tabla forecast_cache). Guarda el JSON
 * crudo de Open-Meteo por coordenada; la rellena ForecastPrefetchScheduler.
 */
@Entity
@Table(name = "forecast_cache")
public class ForecastCacheJpaEntity {

    @Id
    @Column(name = "coord_key")
    private String coordKey;          // "lat,lon"

    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lon;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;           // JSON crudo de OpenMeteoResponse

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    protected ForecastCacheJpaEntity() {}

    public ForecastCacheJpaEntity(String coordKey, double lat, double lon,
                                  String payload, LocalDateTime fetchedAt) {
        this.coordKey = coordKey;
        this.lat = lat;
        this.lon = lon;
        this.payload = payload;
        this.fetchedAt = fetchedAt;
    }

    public String getCoordKey()        { return coordKey; }
    public double getLat()             { return lat; }
    public double getLon()             { return lon; }
    public String getPayload()         { return payload; }
    public LocalDateTime getFetchedAt(){ return fetchedAt; }

    public void setPayload(String payload)          { this.payload = payload; }
    public void setFetchedAt(LocalDateTime fetchedAt){ this.fetchedAt = fetchedAt; }
}
