package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

/**
 * Fila de la caché persistente de forecast (tabla forecast_cache). Guarda el JSON
 * crudo de Open-Meteo por coordenada; la rellena ForecastPrefetchScheduler.
 */
@Entity
@Table(name = "forecast_cache")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ForecastCacheJpaEntity {

    @Id
    @Column(name = "coord_key")
    private String coordKey;          // "lat,lon"

    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lon;

    @Column(columnDefinition = "TEXT", nullable = false)
    @Setter
    private String payload;           // JSON crudo de OpenMeteoResponse

    @Column(name = "fetched_at", nullable = false)
    @Setter
    private LocalDateTime fetchedAt;

}
