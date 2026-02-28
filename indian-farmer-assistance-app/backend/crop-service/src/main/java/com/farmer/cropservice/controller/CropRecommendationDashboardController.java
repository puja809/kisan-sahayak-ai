package com.farmer.cropservice.controller;

import com.farmer.cropservice.dto.CropRecommendationDashboardRequest;
import com.farmer.cropservice.dto.CropRecommendationDashboardResponse;
import com.farmer.cropservice.service.CropRecommendationDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/crops/dashboard")
@RequiredArgsConstructor
@Slf4j
public class CropRecommendationDashboardController {
    
    private final CropRecommendationDashboardService dashboardService;
    
    @PostMapping("/recommendations")
    public ResponseEntity<CropRecommendationDashboardResponse> getDashboardRecommendations(
            @RequestBody CropRecommendationDashboardRequest request) {
        log.info("Getting dashboard recommendations for location: {}, {}", 
                 request.getLatitude(), request.getLongitude());
        
        CropRecommendationDashboardResponse response = dashboardService.getDashboardData(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/recommendations")
    public ResponseEntity<CropRecommendationDashboardResponse> getDashboardRecommendationsGet(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(required = false) String season,
            @RequestParam(required = false) String previousCrop) {
        log.info("Getting dashboard recommendations for location: {}, {}", latitude, longitude);
        
        CropRecommendationDashboardRequest request = new CropRecommendationDashboardRequest();
        request.setLatitude(latitude);
        request.setLongitude(longitude);
        request.setSeason(season);
        request.setPreviousCrop(previousCrop);
        
        CropRecommendationDashboardResponse response = dashboardService.getDashboardData(request);
        return ResponseEntity.ok(response);
    }
}
