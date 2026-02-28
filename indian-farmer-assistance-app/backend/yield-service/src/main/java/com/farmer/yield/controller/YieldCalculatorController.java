package com.farmer.yield.controller;

import com.farmer.yield.dto.YieldCalculationRequest;
import com.farmer.yield.dto.YieldCalculationResponse;
import com.farmer.yield.service.YieldCalculatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for yield calculation endpoints
 */
@RestController
@RequestMapping("/api/v1/crops/yield/calculate")
@RequiredArgsConstructor
@Slf4j
public class YieldCalculatorController {

    private final YieldCalculatorService yieldCalculatorService;

    /**
     * Calculate yield based on commodity, farm size, and investment amount
     * 
     * POST /api/v1/crops/yield/calculate
     * 
     * @param request Yield calculation request
     * @return Yield calculation response with estimates
     */
    @PostMapping
    public ResponseEntity<YieldCalculationResponse> calculateYield(
            @Valid @RequestBody YieldCalculationRequest request) {
        
        log.info("Received yield calculation request for commodity: {}", request.getCommodity());
        
        try {
            YieldCalculationResponse response = yieldCalculatorService.calculateYield(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid commodity: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    YieldCalculationResponse.builder()
                            .success(false)
                            .message("Error: " + e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("Error calculating yield: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(
                    YieldCalculationResponse.builder()
                            .success(false)
                            .message("Error calculating yield: " + e.getMessage())
                            .build()
            );
        }
    }
}
