package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.geo.CountryCatalog;
import com.meteomontana.api.domain.model.Country;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Catálogo de países y regiones para los desplegables de las apps.
 *
 * <p>Público: hace falta antes de iniciar sesión, al proponer una escuela.
 */
@RestController
@RequestMapping("/api/geo")
public class GeoController {

    @GetMapping("/countries")
    public List<Country> countries() {
        return CountryCatalog.all();
    }
}
