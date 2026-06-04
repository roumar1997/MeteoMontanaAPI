package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.forecast.ForecastResponse;
import com.meteomontana.api.application.forecast.GetForecastByLocationUseCase;
import com.meteomontana.api.application.forecast.GetTodayScoresUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ForecastController {

    private final GetForecastByLocationUseCase useCase;
    private final GetTodayScoresUseCase todayScoresUseCase;

    public ForecastController(GetForecastByLocationUseCase useCase,
                              GetTodayScoresUseCase todayScoresUseCase) {
        this.useCase = useCase;
        this.todayScoresUseCase = todayScoresUseCase;
    }

    @GetMapping("/forecast/today-scores")
    public List<GetTodayScoresUseCase.SchoolScoreDto> todayScores(
            @RequestParam("ids") List<String> ids) {
        return todayScoresUseCase.forIds(ids);
    }

    @GetMapping("/forecast/by-location")
    public ForecastResponse byLocation(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(required = false) String rockType) {
        return useCase.execute(lat, lon, rockType);
    }
}
