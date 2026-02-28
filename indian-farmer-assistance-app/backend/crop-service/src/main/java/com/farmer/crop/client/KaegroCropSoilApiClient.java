package com.farmer.crop.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * API client for Kaegro soil data endpoint.
 * Fetches soil details including texture, physical, chemical, and water properties.
 * 
 * API: https://kaegro.com/farms/api/soil/?lat={latitude}&lon={longitude}
 */
@Slf4j
@Component
public class KaegroCropSoilApiClient {

    private final RestTemplate restTemplate;

    @Value("${kaegro.api.base-url:https://kaegro.com/farms/api}")
    private String baseUrl;

    @Value("${kaegro.api.timeout:10000}")
    private long timeout;

    public KaegroCropSoilApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Fetch soil data for given coordinates
     * 
     * @param latitude GPS latitude
     * @param longitude GPS longitude
     * @return Soil data response
     */
    public SoilDataResponse getSoilData(Double latitude, Double longitude) {
        try {
            String url = String.format("%s/soil/?lat=%.2f&lon=%.2f", baseUrl, latitude, longitude);
            log.info("Fetching soil data from Kaegro API: {}", url);

            SoilDataResponse response = restTemplate.getForObject(url, SoilDataResponse.class);
            
            if (response != null) {
                log.info("Successfully fetched soil data for coordinates: {}, {}", latitude, longitude);
            }
            
            return response;
        } catch (Exception e) {
            log.error("Error fetching soil data from Kaegro API: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Soil data response from Kaegro API
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SoilDataResponse {
        private Location location;
        private SoilType soilType;
        private Physical physical;
        private Chemical chemical;
        private Water water;
        private Meta meta;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Location {
        private Double lat;
        private Double lon;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SoilType {
        @JsonProperty("texture_class")
        private String textureClass;

        @JsonProperty("fao_classification")
        private String faoClassification;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Physical {
        @JsonProperty("sand_pct")
        private Double sandPct;

        @JsonProperty("silt_pct")
        private Double siltPct;

        @JsonProperty("clay_pct")
        private Double clayPct;

        @JsonProperty("bulk_density_g_cm3")
        private Double bulkDensityGCm3;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Chemical {
        @JsonProperty("ph_h2o")
        private Double phH2o;

        @JsonProperty("organic_matter_pct")
        private Double organicMatterPct;

        @JsonProperty("nitrogen_g_kg")
        private Double nitrogenGKg;

        @JsonProperty("cec_cmol_kg")
        private Double cecCmolKg;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Water {
        @JsonProperty("capacity_field_vol_pct")
        private Double capacityFieldVolPct;

        @JsonProperty("capacity_wilt_vol_pct")
        private Double capacityWiltVolPct;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Meta {
        @JsonProperty("latency_seconds")
        private Double latencySeconds;
    }
}
