package com.farmer.apigateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizationFilterTest {

    private AuthorizationFilter authorizationFilter;

    @BeforeEach
    void setUp() {
        authorizationFilter = new AuthorizationFilter();
    }

    @Test
    void testAdminCanAccessAdminEndpoint() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/admin/users")
                .header("X-User-Id", "admin123")
                .header("X-User-Role", "ADMIN")
                .build();
        
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        
        var filter = authorizationFilter.apply(new AuthorizationFilter.Config());
        var result = filter.filter(exchange, e -> Mono.empty());
        
        StepVerifier.create(result)
                .expectComplete()
                .verify();
        
        assertNotEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void testFarmerCannotAccessAdminEndpoint() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/admin/users")
                .header("X-User-Id", "farmer123")
                .header("X-User-Role", "FARMER")
                .build();
        
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        
        var filter = authorizationFilter.apply(new AuthorizationFilter.Config());
        var result = filter.filter(exchange, e -> Mono.empty());
        
        StepVerifier.create(result)
                .expectComplete()
                .verify();
        
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void testFarmerCanAccessFarmerEndpoint() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/profile")
                .header("X-User-Id", "farmer123")
                .header("X-User-Role", "FARMER")
                .build();
        
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        
        var filter = authorizationFilter.apply(new AuthorizationFilter.Config());
        var result = filter.filter(exchange, e -> Mono.empty());
        
        StepVerifier.create(result)
                .expectComplete()
                .verify();
        
        assertNotEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void testAdminCanAccessFarmerEndpoint() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/profile")
                .header("X-User-Id", "admin123")
                .header("X-User-Role", "ADMIN")
                .build();
        
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        
        var filter = authorizationFilter.apply(new AuthorizationFilter.Config());
        var result = filter.filter(exchange, e -> Mono.empty());
        
        StepVerifier.create(result)
                .expectComplete()
                .verify();
        
        assertNotEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void testMissingRoleHeaderReturnsForbidden() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/profile")
                .header("X-User-Id", "user123")
                .build();
        
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        
        var filter = authorizationFilter.apply(new AuthorizationFilter.Config());
        var result = filter.filter(exchange, e -> Mono.empty());
        
        StepVerifier.create(result)
                .expectComplete()
                .verify();
        
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void testWeatherEndpointAccessibleByBothRoles() {
        // Test FARMER access
        MockServerHttpRequest farmerRequest = MockServerHttpRequest.get("/api/v1/weather/forecast/karnataka")
                .header("X-User-Id", "farmer123")
                .header("X-User-Role", "FARMER")
                .build();
        
        ServerWebExchange farmerExchange = MockServerWebExchange.from(farmerRequest);
        var filter = authorizationFilter.apply(new AuthorizationFilter.Config());
        var result = filter.filter(farmerExchange, e -> Mono.empty());
        
        StepVerifier.create(result)
                .expectComplete()
                .verify();
        
        assertNotEquals(HttpStatus.FORBIDDEN, farmerExchange.getResponse().getStatusCode());
        
        // Test ADMIN access
        MockServerHttpRequest adminRequest = MockServerHttpRequest.get("/api/v1/weather/forecast/karnataka")
                .header("X-User-Id", "admin123")
                .header("X-User-Role", "ADMIN")
                .build();
        
        ServerWebExchange adminExchange = MockServerWebExchange.from(adminRequest);
        result = filter.filter(adminExchange, e -> Mono.empty());
        
        StepVerifier.create(result)
                .expectComplete()
                .verify();
        
        assertNotEquals(HttpStatus.FORBIDDEN, adminExchange.getResponse().getStatusCode());
    }
}
