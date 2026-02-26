package com.farmer.weather.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class WeatherApiResponse {
    private Location location;
    private Current current;
    private Forecast forecast;

    @Data
    public static class Location {
        private String name;
        private String region;
        private String country;
        private double lat;
        private double lon;
        @JsonProperty("tz_id")
        private String tzId;
        @JsonProperty("localtime_epoch")
        private long localtimeEpoch;
        private String localtime;
    }

    @Data
    public static class Current {
        @JsonProperty("last_updated_epoch")
        private long lastUpdatedEpoch;
        @JsonProperty("last_updated")
        private String lastUpdated;
        @JsonProperty("temp_c")
        private double tempC;
        @JsonProperty("temp_f")
        private double tempF;
        @JsonProperty("is_day")
        private int isDay;
        private Condition condition;
        @JsonProperty("wind_mph")
        private double windMph;
        @JsonProperty("wind_kph")
        private double windKph;
        @JsonProperty("wind_degree")
        private int windDegree;
        @JsonProperty("wind_dir")
        private String windDir;
        @JsonProperty("pressure_mb")
        private double pressureMb;
        @JsonProperty("pressure_in")
        private double pressureIn;
        @JsonProperty("precip_mm")
        private double precipMm;
        @JsonProperty("precip_in")
        private double precipIn;
        private int humidity;
        private int cloud;
        @JsonProperty("feelslike_c")
        private double feelslikeC;
        @JsonProperty("feelslike_f")
        private double feelslikeF;
        @JsonProperty("vis_km")
        private double visKm;
        @JsonProperty("vis_miles")
        private double visMiles;
        private double uv;
        @JsonProperty("gust_mph")
        private double gustMph;
        @JsonProperty("gust_kph")
        private double gustKph;
    }

    @Data
    public static class Condition {
        private String text;
        private String icon;
        private int code;
    }

    @Data
    public static class Forecast {
        @JsonProperty("forecastday")
        private List<ForecastDay> forecastday;
    }

    @Data
    public static class ForecastDay {
        private String date;
        @JsonProperty("date_epoch")
        private long dateEpoch;
        private Day day;
        private Astro astro;
        private List<Hour> hour;
    }

    @Data
    public static class Day {
        @JsonProperty("maxtemp_c")
        private double maxtempC;
        @JsonProperty("maxtemp_f")
        private double maxtempF;
        @JsonProperty("mintemp_c")
        private double mintempC;
        @JsonProperty("mintemp_f")
        private double mintempF;
        @JsonProperty("avgtemp_c")
        private double avgtempC;
        @JsonProperty("avgtemp_f")
        private double avgtempF;
        @JsonProperty("maxwind_mph")
        private double maxwindMph;
        @JsonProperty("maxwind_kph")
        private double maxwindKph;
        @JsonProperty("totalprecip_mm")
        private double totalprecipMm;
        @JsonProperty("totalprecip_in")
        private double totalprecipIn;
        @JsonProperty("totalsnow_cm")
        private double totalsnowCm;
        @JsonProperty("avgvis_km")
        private double avgvisKm;
        @JsonProperty("avgvis_miles")
        private double avgvisMiles;
        @JsonProperty("avghumidity")
        private double avghumidity;
        @JsonProperty("daily_will_it_rain")
        private int dailyWillItRain;
        @JsonProperty("daily_chance_of_rain")
        private int dailyChanceOfRain;
        @JsonProperty("daily_will_it_snow")
        private int dailyWillItSnow;
        @JsonProperty("daily_chance_of_snow")
        private int dailyChanceOfSnow;
        private Condition condition;
        private double uv;
    }

    @Data
    public static class Astro {
        private String sunrise;
        private String sunset;
        private String moonrise;
        private String moonset;
        @JsonProperty("moon_phase")
        private String moonPhase;
        @JsonProperty("moon_illumination")
        private String moonIllumination;
        @JsonProperty("is_moon_up")
        private int isMoonUp;
        @JsonProperty("is_sun_up")
        private int isSunUp;
    }

    @Data
    public static class Hour {
        @JsonProperty("time_epoch")
        private long timeEpoch;
        private String time;
        @JsonProperty("temp_c")
        private double tempC;
        @JsonProperty("temp_f")
        private double tempF;
        @JsonProperty("is_day")
        private int isDay;
        private Condition condition;
        @JsonProperty("wind_mph")
        private double windMph;
        @JsonProperty("wind_kph")
        private double windKph;
        @JsonProperty("wind_degree")
        private int windDegree;
        @JsonProperty("wind_dir")
        private String windDir;
        @JsonProperty("pressure_mb")
        private double pressureMb;
        @JsonProperty("pressure_in")
        private double pressureIn;
        @JsonProperty("precip_mm")
        private double precipMm;
        @JsonProperty("precip_in")
        private double precipIn;
        private int humidity;
        private int cloud;
        @JsonProperty("feelslike_c")
        private double feelslikeC;
        @JsonProperty("feelslike_f")
        private double feelslikeF;
        @JsonProperty("windchill_c")
        private double windchillC;
        @JsonProperty("windchill_f")
        private double windchillF;
        @JsonProperty("heatindex_c")
        private double heatindexC;
        @JsonProperty("heatindex_f")
        private double heatindexF;
        @JsonProperty("dewpoint_c")
        private double dewpointC;
        @JsonProperty("dewpoint_f")
        private double dewpointF;
        @JsonProperty("will_it_rain")
        private int willItRain;
        @JsonProperty("chance_of_rain")
        private int chanceOfRain;
        @JsonProperty("will_it_snow")
        private int willItSnow;
        @JsonProperty("chance_of_snow")
        private int chanceOfSnow;
        @JsonProperty("vis_km")
        private double visKm;
        @JsonProperty("vis_miles")
        private double visMiles;
        @JsonProperty("gust_mph")
        private double gustMph;
        @JsonProperty("gust_kph")
        private double gustKph;
        private double uv;
    }
}