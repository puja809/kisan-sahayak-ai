package com.farmer.weather.config;

import com.farmer.weather.dto.CurrentWeatherDto;
import com.farmer.weather.dto.SevenDayForecastDto;
import com.farmer.weather.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherMcpTools {

    private final WeatherService weatherService;

    @Tool(description = "Get the current weather for a specific district and state in India. Returns temperature, humidity, wind, and alert information.")
    public CurrentWeatherDto getCurrentWeather(String district, String state) {
        log.info("MCP Tool Executing getCurrentWeather for District: {}, State: {}", district, state);
        if (state == null || district == null) {
            return null;
        }
        return weatherService.getCurrentWeather(district, state, null, null).block();
    }

    @Tool(description = "Get a 7-day weather forecast for a specific district and state in India. Useful for agricultural planning over the next week.")
    public SevenDayForecastDto getSevenDayForecast(String district, String state) {
        log.info("MCP Tool Executing getSevenDayForecast for District: {}, State: {}", district, state);
        if (state == null || district == null) {
            return null;
        }
        return weatherService.getSevenDayForecast(district, state, null, null).block();
    }

    @Configuration
    static class WeatherMcpToolsConfig {
        @Bean
        public MethodToolCallbackProvider weatherToolCallbackProvider(WeatherMcpTools weatherMcpTools) {
            return MethodToolCallbackProvider.builder().toolObjects(weatherMcpTools).build();
        }
    }
}
