package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.infrastructure.geo.NominatimGeocoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Búsqueda de localidades por nombre (para el buscador del tiempo). Público. */
@RestController
@RequestMapping("/api")
public class GeocodeController {

    private final NominatimGeocoder geocoder;

    public GeocodeController(NominatimGeocoder geocoder) {
        this.geocoder = geocoder;
    }

    @GetMapping("/geocode")
    public List<NominatimGeocoder.Place> geocode(@RequestParam("q") String q) {
        return geocoder.search(q);
    }
}
