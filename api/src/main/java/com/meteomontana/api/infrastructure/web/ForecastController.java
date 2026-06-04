package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.forecast.ForecastResponse;
import com.meteomontana.api.application.forecast.GetForecastByLocationUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ForecastController {

    private final GetForecastByLocationUseCase useCase;

    public ForecastController(GetForecastByLocationUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/forecast/by-location")
    public ForecastResponse byLocation(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(required = false) String rockType) {
        return useCase.execute(lat, lon, rockType);
    }
}
