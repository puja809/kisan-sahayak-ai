package com.farmer.cropservice.config;

import com.farmer.cropservice.dto.CropRecommendationDashboardRequest;
import com.farmer.cropservice.dto.CropRecommendationDashboardResponse;
import com.farmer.cropservice.service.CropRecommendationDashboardService;
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
public class CropMcpTools {

    private final CropRecommendationDashboardService cropRecommendationDashboardService;

    @Tool(description = "Get comprehensive crop recommendations based on location (latitude and longitude) and season. Internally fetches soil data and weather forecast, then calls ML models to determine the best crop to plant, fertilizer dosage, and optimal crop rotation.")
    public CropRecommendationDashboardResponse getCropRecommendations(
            Double latitude,
            Double longitude,
            String season,
            String previousCrop) {
        log.info("MCP Tool Executing getCropRecommendations for lat: {}, lon: {}, season: {}", latitude, longitude,
                season);

        CropRecommendationDashboardRequest request = new CropRecommendationDashboardRequest();
        request.setLatitude(latitude);
        request.setLongitude(longitude);
        request.setSeason(season != null ? season : "Kharif");
        request.setPreviousCrop(previousCrop);

        return cropRecommendationDashboardService.getDashboardData(request);
    }

    @Configuration
    static class CropMcpToolsConfig {
        @Bean
        public MethodToolCallbackProvider cropToolCallbackProvider(CropMcpTools cropMcpTools) {
            return MethodToolCallbackProvider.builder().toolObjects(cropMcpTools).build();
        }
    }
}
