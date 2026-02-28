package com.farmer.mandi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherDataResponse {
    @JsonProperty("current")
    private CurrentWeather current;
    
    @JsonProperty("forecast")
    private Forecast forecast;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentWeather {
        @JsonProperty("temp_c")
        private Double tempC;
        
        @JsonProperty("humidity")
        private Double humidity;
        
        @JsonProperty("precip_mm")
        private Double precipMm;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Forecast {
        @JsonProperty("forecastday")
        private java.util.List<ForecastDay> forecastday;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForecastDay {
        @JsonProperty("day")
        private DayData day;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayData {
        @JsonProperty("avgtemp_c")
        private Double avgTempC;
        
        @JsonProperty("avghumidity")
        private Double avgHumidity;
        
        @JsonProperty("totalprecip_mm")
        private Double totalPrecipMm;
    }
}
