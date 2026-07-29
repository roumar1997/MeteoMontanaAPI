package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.forecast.ForecastResponse;
import com.meteomontana.api.application.forecast.GetForecastByLocationUseCase;
import com.meteomontana.api.application.forecast.GetRangeScoresUseCase;
import com.meteomontana.api.application.forecast.GetTodayScoresUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ForecastController {

    private final GetForecastByLocationUseCase useCase;
    private final GetTodayScoresUseCase todayScoresUseCase;
    private final GetRangeScoresUseCase rangeScoresUseCase;

    @GetMapping("/forecast/today-scores")
    public List<GetTodayScoresUseCase.SchoolScoreDto> todayScores(
            @RequestParam("ids") List<String> ids) {
        return todayScoresUseCase.forIds(ids);
    }

    /** Score de un tramo de días elegidos (selector de días de la lista). */
    @GetMapping("/forecast/range-scores")
    public List<GetRangeScoresUseCase.RangeScoreDto> rangeScores(
            @RequestParam("ids") List<String> ids,
            @RequestParam("dates") List<String> dates) {
        return rangeScoresUseCase.forIds(ids, dates);
    }

    @GetMapping("/forecast/by-location")
    public ForecastResponse byLocation(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(required = false) String rockType) {
        return useCase.execute(lat, lon, rockType);
    }
}
