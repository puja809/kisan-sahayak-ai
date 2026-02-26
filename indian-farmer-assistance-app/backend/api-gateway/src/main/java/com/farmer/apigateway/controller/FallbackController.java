package com.farmer.apigateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Fallback Controller
 * Provides fallback responses when downstream services are unavailable
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    private static final Logger logger = LoggerFactory.getLogger(FallbackController.class);

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> userServiceFallback() {
        logger.warn("User Service fallback triggered");
        return buildFallbackResponse("User Service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/weather")
    public ResponseEntity<Map<String, Object>> weatherServiceFallback() {
        logger.warn("Weather Service fallback triggered");
        return buildFallbackResponse("Weather Service is temporarily unavailable. Please check cached data.");
    }

    @GetMapping("/crops")
    public ResponseEntity<Map<String, Object>> cropServiceFallback() {
        logger.warn("Crop Service fallback triggered");
        return buildFallbackResponse("Crop Service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/schemes")
    public ResponseEntity<Map<String, Object>> schemeServiceFallback() {
        logger.warn("Scheme Service fallback triggered");
        return buildFallbackResponse("Scheme Service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/mandi")
    public ResponseEntity<Map<String, Object>> mandiServiceFallback() {
        logger.warn("Mandi Service fallback triggered");
        return buildFallbackResponse("Mandi Service is temporarily unavailable. Please check cached prices.");
    }

    @GetMapping("/iot")
    public ResponseEntity<Map<String, Object>> iotServiceFallback() {
        logger.warn("IoT Service fallback triggered");
        return buildFallbackResponse("IoT Service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/admin")
    public ResponseEntity<Map<String, Object>> adminServiceFallback() {
        logger.warn("Admin Service fallback triggered");
        return buildFallbackResponse("Admin Service is temporarily unavailable. Please try again later.");
    }

    @GetMapping("/ai")
    public ResponseEntity<Map<String, Object>> aiServiceFallback() {
        logger.warn("AI Service fallback triggered");
        return buildFallbackResponse("AI Service is temporarily unavailable. Please try again later.");
    }

    /**
     * Build fallback response
     */
    private ResponseEntity<Map<String, Object>> buildFallbackResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SERVICE_UNAVAILABLE");
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
