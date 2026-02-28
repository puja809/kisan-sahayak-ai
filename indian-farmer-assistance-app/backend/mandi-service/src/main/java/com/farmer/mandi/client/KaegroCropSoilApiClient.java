package com.farmer.mandi.client;

import com.farmer.mandi.dto.SoilDataResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@RequiredArgsConstructor
@Slf4j
public class KaegroCropSoilApiClient {
    
    private final RestTemplate restTemplate;
    private static final String KAEGRO_API_URL = "https://www.kaegro.com/farms/api/soil";
    
    public SoilDataResponse getSoilData(Double latitude, Double longitude) {
        try {
            double latTrimmed = new BigDecimal(latitude) .setScale(2, RoundingMode.DOWN) .doubleValue();
            double lonTrimmed = new BigDecimal(longitude) .setScale(2, RoundingMode.DOWN) .doubleValue();
            String url = String.format("%s?lat=%.2f&lon=%.2f", KAEGRO_API_URL, latTrimmed, lonTrimmed);
            log.info("Fetching soil data from Kaegro: {}", url);
            
            SoilDataResponse response = restTemplate.getForObject(url, SoilDataResponse.class);
            log.info("Soil data response: {}", response);
            log.info("Soil data fetched successfully");
            return response;
        } catch (Exception e) {
            log.error("Error fetching soil data from Kaegro", e);
            throw new RuntimeException("Failed to fetch soil data", e);
        }
    }
}
