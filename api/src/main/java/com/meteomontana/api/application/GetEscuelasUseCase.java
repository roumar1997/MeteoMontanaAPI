package com.meteomontana.api.application;
import com.meteomontana.api.domain.model.Escuela;
import com.meteomontana.api.domain.port.EscuelaRepository;
import com.meteomontana.api.domain.util.GeoDistance;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class GetEscuelasUseCase {
    private final EscuelaRepository repository;
    public GetEscuelasUseCase(EscuelaRepository repository){
        this.repository = repository;
    }

    public List<Escuela> execute(String ccaa, String estilo, List<String> rocas ,Double lat, Double lon, Double radioKm) {
        boolean filtroDistanciaActivo = lat != null && lon != null && radioKm != null;
        boolean filtroRocaActivo = rocas != null && !rocas.isEmpty();

        return repository.findAll().stream()
                .filter(e -> ccaa == null || ccaa.isBlank()
                        || ccaa.equalsIgnoreCase(e.getCcaa()))
                .filter(e -> estilo == null || estilo.isBlank()
                        || estilo.equalsIgnoreCase(e.getEstilo()))
                .filter(e -> !filtroRocaActivo
                        || rocas.stream().anyMatch(r -> r.equalsIgnoreCase(e.getRoca()))
                )
                .filter(e -> !filtroDistanciaActivo
                        || GeoDistance.haversineKM(lat, lon, e.getLat(), e.getLon()) <= radioKm)
                .toList();
    }
}
