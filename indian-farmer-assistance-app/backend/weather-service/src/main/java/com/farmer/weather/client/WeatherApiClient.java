package com.farmer.weather.client;

import com.farmer.weather.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class WeatherApiClient {

    private static final Logger logger = LoggerFactory.getLogger(WeatherApiClient.class);

    private final WebClient webClient;
    private final String apiKey;

    public WeatherApiClient(WebClient.Builder webClientBuilder,
                            @Value("${weatherapi.base-url:https://api.weatherapi.com/v1}") String baseUrl,
                            @Value("${weatherapi.api-key}") String apiKey) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    public Mono<SevenDayForecastDto> getSevenDayForecast(String district, String state) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/forecast.json")
                        .queryParam("key", apiKey)
                        .queryParam("q", district + "," + state)
                        .queryParam("days", 7)
                        .queryParam("aqi", "no")
                        .queryParam("alerts", "no")
                        .build())
                .retrieve()
                .bodyToMono(WeatherApiResponse.class)
                .map(response -> mapToSevenDayForecastDto(response, district, state))
                .doOnError(e -> logger.error("Error fetching 7-day forecast for {}, {}: {}", district, state, e.getMessage()));
    }

    public Mono<CurrentWeatherDto> getCurrentWeather(String district, String state) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/current.json")
                        .queryParam("key", apiKey)
                        .queryParam("q", district + "," + state)
                        .queryParam("aqi", "no")
                        .build())
                .retrieve()
                .bodyToMono(WeatherApiResponse.class)
                .map(response -> mapToCurrentWeatherDto(response, district, state))
                .doOnError(e -> logger.error("Error fetching current weather for {}, {}: {}", district, state, e.getMessage()));
    }

    public Mono<NowcastDto> getNowcast(String district, String state) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/forecast.json")
                        .queryParam("key", apiKey)
                        .queryParam("q", district + "," + state)
                        .queryParam("days", 1)
                        .queryParam("aqi", "no")
                        .queryParam("alerts", "no")
                        .build())
                .retrieve()
                .bodyToMono(WeatherApiResponse.class)
                .map(response -> mapToNowcastDto(response, district, state))
                .doOnError(e -> logger.error("Error fetching nowcast for {}, {}: {}", district, state, e.getMessage()));
    }

    public Mono<WeatherAlertDto> getWeatherAlerts(String district, String state) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/forecast.json")
                        .queryParam("key", apiKey)
                        .queryParam("q", district + "," + state)
                        .queryParam("days", 1)
                        .queryParam("aqi", "no")
                        .queryParam("alerts", "yes")
                        .build())
                .retrieve()
                .bodyToMono(WeatherApiResponse.class)
                .map(response -> mapToWeatherAlertDto(response, district, state))
                .doOnError(e -> logger.error("Error fetching alerts for {}, {}: {}", district, state, e.getMessage()));
    }

    public Mono<RainfallStatsDto> getRainfallStats(String district, String state) {
        return Mono.empty();
    }

    public Mono<AgrometAdvisoryDto> getAgrometAdvisories(String district, String state) {
        return Mono.empty();
    }

    public Mono<List<AwsArgDataDto>> getAwsArgData(String district, String state) {
        return Mono.empty();
    }

    private SevenDayForecastDto mapToSevenDayForecastDto(WeatherApiResponse response, String district, String state) {
        SevenDayForecastDto dto = new SevenDayForecastDto();
        dto.setDistrict(district);
        dto.setState(state);
        dto.setFetchedAt(LocalDateTime.now());
        dto.setValidFrom(LocalDateTime.now());
        dto.setValidUntil(LocalDateTime.now().plusDays(7));

        if (response.getForecast() != null && response.getForecast().getForecastday() != null) {
            List<ForecastDayDto> forecastDays = response.getForecast().getForecastday().stream()
                    .map(this::mapToForecastDayDto)
                    .collect(Collectors.toList());
            dto.setForecastDays(forecastDays);
        }

        return dto;
    }

    private ForecastDayDto mapToForecastDayDto(WeatherApiResponse.ForecastDay fd) {
        ForecastDayDto dto = new ForecastDayDto();
        dto.setDate(LocalDate.parse(fd.getDate()));
        
        if (fd.getDay() != null) {
            dto.setMaxTempCelsius(fd.getDay().getMaxtempC());
            dto.setMinTempCelsius(fd.getDay().getMintempC());
            dto.setRainfallMm(fd.getDay().getTotalprecipMm());
            dto.setWindSpeedKmph(fd.getDay().getMaxwindKph());
            dto.setHumidity0830(fd.getDay().getAvghumidity());
            dto.setHumidity1730(fd.getDay().getAvghumidity());
            dto.setCloudCoverage(0); 
        }
        
        if (fd.getAstro() != null) {
            dto.setSunriseTime(fd.getAstro().getSunrise());
            dto.setSunsetTime(fd.getAstro().getSunset());
            dto.setMoonriseTime(fd.getAstro().getMoonrise());
            dto.setMoonsetTime(fd.getAstro().getMoonset());
        }
        
        return dto;
    }

    private CurrentWeatherDto mapToCurrentWeatherDto(WeatherApiResponse response, String district, String state) {
        CurrentWeatherDto dto = new CurrentWeatherDto();
        dto.setDistrict(district);
        dto.setState(state);
        dto.setObservationTime(LocalDateTime.now());
        
        if (response.getCurrent() != null) {
            dto.setTemperatureCelsius(response.getCurrent().getTempC());
            dto.setHumidity(Double.valueOf(response.getCurrent().getHumidity()));
            dto.setWindSpeedKmph(response.getCurrent().getWindKph());
            dto.setWindDirection(response.getCurrent().getWindDir());
            dto.setCloudCoverage(response.getCurrent().getCloud());
            dto.setRainfall24HoursMm(response.getCurrent().getPrecipMm());
            dto.setPressureHpa(response.getCurrent().getPressureMb());
            dto.setVisibilityKm(response.getCurrent().getVisKm());
            if (response.getCurrent().getCondition() != null) {
                dto.setWeatherDescription(response.getCurrent().getCondition().getText());
            }
        }
        return dto;
    }

    private NowcastDto mapToNowcastDto(WeatherApiResponse response, String district, String state) {
        NowcastDto dto = new NowcastDto();
        dto.setDistrict(district);
        dto.setState(state);
        dto.setIssuedAt(LocalDateTime.now());
        dto.setValidUntil(LocalDateTime.now().plusHours(3));
        
        if (response.getCurrent() != null && response.getCurrent().getCondition() != null) {
            dto.setOverallSituation(response.getCurrent().getCondition().getText());
        }
        
        // Probability of precipitation from forecast day 0 if available
        if (response.getForecast() != null && !response.getForecast().getForecastday().isEmpty()) {
             WeatherApiResponse.ForecastDay today = response.getForecast().getForecastday().get(0);
             if (today.getDay() != null) {
                 dto.setProbabilityOfPrecipitation((double) today.getDay().getDailyChanceOfRain());
             }
        }
        
        return dto;
    }

    private WeatherAlertDto mapToWeatherAlertDto(WeatherApiResponse response, String district, String state) {
        WeatherAlertDto dto = new WeatherAlertDto();
        dto.setDistrict(district);
        dto.setState(state);
        dto.setIssuedAt(LocalDateTime.now());
        
        // WeatherAPI alerts mapping would go here if we had the AlertDto structure
        // For now returning empty alerts list
        dto.setAlerts(Collections.emptyList());
        dto.setOverallAlertLevel("GREEN"); // Default to GREEN

        return dto;
    }
}