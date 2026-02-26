package com.farmer.apigateway.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FallbackControllerTest {

    private FallbackController fallbackController;

    @BeforeEach
    void setUp() {
        fallbackController = new FallbackController();
    }

    @Test
    void testUserServiceFallback() {
        ResponseEntity<Map<String, Object>> response = fallbackController.userServiceFallback();
        
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SERVICE_UNAVAILABLE", response.getBody().get("status"));
        assertTrue(response.getBody().get("message").toString().contains("User Service"));
    }

    @Test
    void testWeatherServiceFallback() {
        ResponseEntity<Map<String, Object>> response = fallbackController.weatherServiceFallback();
        
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SERVICE_UNAVAILABLE", response.getBody().get("status"));
        assertTrue(response.getBody().get("message").toString().contains("Weather Service"));
    }

    @Test
    void testCropServiceFallback() {
        ResponseEntity<Map<String, Object>> response = fallbackController.cropServiceFallback();
        
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SERVICE_UNAVAILABLE", response.getBody().get("status"));
    }

    @Test
    void testSchemeServiceFallback() {
        ResponseEntity<Map<String, Object>> response = fallbackController.schemeServiceFallback();
        
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SERVICE_UNAVAILABLE", response.getBody().get("status"));
    }

    @Test
    void testMandiServiceFallback() {
        ResponseEntity<Map<String, Object>> response = fallbackController.mandiServiceFallback();
        
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SERVICE_UNAVAILABLE", response.getBody().get("status"));
    }

    @Test
    void testIoTServiceFallback() {
        ResponseEntity<Map<String, Object>> response = fallbackController.iotServiceFallback();
        
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SERVICE_UNAVAILABLE", response.getBody().get("status"));
    }

    @Test
    void testAdminServiceFallback() {
        ResponseEntity<Map<String, Object>> response = fallbackController.adminServiceFallback();
        
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SERVICE_UNAVAILABLE", response.getBody().get("status"));
    }

    @Test
    void testAIServiceFallback() {
        ResponseEntity<Map<String, Object>> response = fallbackController.aiServiceFallback();
        
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SERVICE_UNAVAILABLE", response.getBody().get("status"));
    }

    @Test
    void testFallbackResponseContainsTimestamp() {
        ResponseEntity<Map<String, Object>> response = fallbackController.userServiceFallback();
        
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().get("timestamp"));
        assertTrue(response.getBody().get("timestamp") instanceof Long);
    }
}

