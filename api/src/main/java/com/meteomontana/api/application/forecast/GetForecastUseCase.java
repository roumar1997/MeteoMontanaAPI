package com.meteomontana.api.application.forecast;

import com.meteomontana.api.domain.exception.SchoolNotFoundException;
import com.meteomontana.api.domain.model.School;
import com.meteomontana.api.domain.port.SchoolRepository;
import com.meteomontana.api.domain.score.ClimbScoreCalculator;
import com.meteomontana.api.domain.score.RockDryingProfile;
import com.meteomontana.api.infrastructure.weather.OpenMeteoClient;
import com.meteomontana.api.infrastructure.weather.OpenMeteoResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GetForecastUseCase {
    private final SchoolRepository schoolRepository;
    private final OpenMeteoClient openMeteoClient;

    public GetForecastUseCase(SchoolRepository schoolRepository,
                              OpenMeteoClient openMeteoClient){
        this.schoolRepository = schoolRepository;
        this.openMeteoClient = openMeteoClient;
    }

    public  ForecastResponse execute(String schoolId){
        //1-buscar la escuelaç
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new SchoolNotFoundException(schoolId));

        //2- llamar a Open-Meteop
        OpenMeteoResponse weather = openMeteoClient.fetchForecast(school.getLat(), school.getLon());

        //3- calcular el score de cada hora
        List<ForecastResponse.HourForecast> hours = buildHourlyForecast(weather,school.getRockType());

        //4-construir la respuesta
        return new ForecastResponse(
                school.getId(),
                school.getName(),
                school.getLat(),
                school.getLon(),
                hours
        );
    }
    /**
     * Recorre cada hora del forecast, calcula la lluvia reciente
     * (las N horas previas según el tipo de roca) y obtiene el score.
     */
    private List<ForecastResponse.HourForecast>buildHourlyForecast(
            OpenMeteoResponse weather, String rockType){
        OpenMeteoResponse.HourlyData h = weather.hourly();
        int lookback = RockDryingProfile.forRockType(rockType).lookbackHours();
        List<ForecastResponse.HourForecast> result = new ArrayList<>(h.time().size());

        for (int i = 0; i <h.time().size(); i++){
            //lluvia acumulada en las horas previas
            double recentRain = 0;
            for (int j = Math.max(0, i- lookback); j < i; j++){
                recentRain += h.precipitation().get(j);
            }
            double temp = h.temperature().get(i);
            double humidity = h.humidity().get(i);
            double wind = h.windSpeed().get(i);
            double precip = h.precipitation().get(i);
            int prob = h.precipitationProbability().get(i);
            int    cloud    = h.cloudCover() != null ? h.cloudCover().get(i) : 50;
            Double dewPoint = h.dewPoint() != null ? h.dewPoint().get(i) : null;

            int score = ClimbScoreCalculator.calculate(
                    temp,humidity, wind, precip, prob, cloud, dewPoint, recentRain, rockType);

            result.add(new ForecastResponse.HourForecast(
                    h.time().get(i),
                    temp,humidity,wind,precip,prob,cloud,dewPoint,score,ClimbScoreCalculator.label(score)
            ));
        }
        return result;
    }

}
