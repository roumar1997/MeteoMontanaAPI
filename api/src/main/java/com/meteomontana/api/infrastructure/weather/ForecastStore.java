package com.meteomontana.api.infrastructure.weather;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meteomontana.api.infrastructure.persistence.jpa.ForecastCacheJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataForecastCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * Caché PERSISTENTE (Postgres) del forecast crudo de Open-Meteo. Guarda el JSON
 * tal cual lo devuelve Open-Meteo; el score y la "hora actual" se recalculan al
 * servir, así que un dato de hace un rato sigue siendo correcto. La rellena
 * {@code ForecastPrefetchScheduler} cada hora; sobrevive a los redeploys.
 */
@Component
@RequiredArgsConstructor
public class ForecastStore {

    private static final Logger log = LoggerFactory.getLogger(ForecastStore.class);

    private final SpringDataForecastCacheRepository repo;
    private final ObjectMapper mapper;

    /** Clave idéntica a la de la caché en memoria (#lat + ',' + #lon). */
    static String key(double lat, double lon) { return lat + "," + lon; }

    /** Devuelve el forecast guardado si NO es más viejo que maxAge. */
    @Transactional(readOnly = true)
    public Optional<OpenMeteoResponse> getFresh(double lat, double lon, Duration maxAge) {
        return repo.findById(key(lat, lon))
                .filter(e -> e.getFetchedAt().isAfter(LocalDateTime.now().minus(maxAge)))
                .map(this::deserialize)
                .filter(r -> r != null);
    }

    /** Devuelve el forecast guardado sin importar su antigüedad (fallback si
     *  Open-Meteo está caído/limitado: mejor un dato viejo que un 503). */
    @Transactional(readOnly = true)
    public Optional<OpenMeteoResponse> getAny(double lat, double lon) {
        return repo.findById(key(lat, lon))
                .map(this::deserialize)
                .filter(r -> r != null);
    }

    /** Inserta o actualiza el forecast de esa coordenada con la hora actual. */
    @Transactional
    public void save(double lat, double lon, OpenMeteoResponse r) {
        if (r == null) return;
        try {
            String json = mapper.writeValueAsString(r);
            String k = key(lat, lon);
            ForecastCacheJpaEntity e = repo.findById(k)
                    .orElseGet(() -> new ForecastCacheJpaEntity(k, lat, lon, json, LocalDateTime.now()));
            e.setPayload(json);
            e.setFetchedAt(LocalDateTime.now());
            repo.save(e);
        } catch (Exception ex) {
            log.warn("No se pudo guardar el forecast de {},{}: {}", lat, lon, ex.toString());
        }
    }

    private OpenMeteoResponse deserialize(ForecastCacheJpaEntity e) {
        try {
            return mapper.readValue(e.getPayload(), OpenMeteoResponse.class);
        } catch (Exception ex) {
            log.warn("Forecast guardado corrupto en {}: {}", e.getCoordKey(), ex.toString());
            return null;
        }
    }
}
