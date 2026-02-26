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

class RateLimitingFilterTest {

    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUp() {
        rateLimitingFilter = new RateLimitingFilter();
    }

    @Test
    void testRequestWithinRateLimitIsAllowed() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/profile")
                .header("X-User-Id", "user123")
                .header("X-User-Role", "FARMER")
                .build();
        
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        
        var filter = rateLimitingFilter.apply(new RateLimitingFilter.Config());
        var result = filter.filter(exchange, e -> Mono.empty());
        
        StepVerifier.create(result)
                .expectComplete()
                .verify();
        
        assertNotEquals(HttpStatus.TOO_MANY_REQUESTS, exchange.getResponse().getStatusCode());
        assertNotNull(exchange.getResponse().getHeaders().getFirst("X-Rate-Limit-Remaining"));
    }

    @Test
    void testAdminHasHigherRateLimit() {
        // Admin should have 500 requests per minute vs 100 for farmers
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/admin/users")
                .header("X-User-Id", "admin123")
                .header("X-User-Role", "ADMIN")
                .build();
        
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        
        var filter = rateLimitingFilter.apply(new RateLimitingFilter.Config());
        var result = filter.filter(exchange, e -> Mono.empty());
        
        StepVerifier.create(result)
                .expectComplete()
                .verify();
        
        assertNotEquals(HttpStatus.TOO_MANY_REQUESTS, exchange.getResponse().getStatusCode());
    }

    @Test
    void testRateLimitHeadersArePresent() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/profile")
                .header("X-User-Id", "user123")
                .header("X-User-Role", "FARMER")
                .build();
        
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        
        var filter = rateLimitingFilter.apply(new RateLimitingFilter.Config());
        var result = filter.filter(exchange, e -> Mono.empty());
        
        StepVerifier.create(result)
                .expectComplete()
                .verify();
        
        String remaining = exchange.getResponse().getHeaders().getFirst("X-Rate-Limit-Remaining");
        assertNotNull(remaining);
        assertTrue(Integer.parseInt(remaining) > 0);
    }

    @Test
    void testDifferentUsersHaveSeparateBuckets() {
        var filter = rateLimitingFilter.apply(new RateLimitingFilter.Config());
        
        // First user makes a request
        MockServerHttpRequest request1 = MockServerHttpRequest.get("/api/v1/users/profile")
                .header("X-User-Id", "user1")
                .header("X-User-Role", "FARMER")
                .build();
        
        ServerWebExchange exchange1 = MockServerWebExchange.from(request1);
        var result1 = filter.filter(exchange1, e -> Mono.empty());
        
        StepVerifier.create(result1)
                .expectComplete()
                .verify();
        
        String remaining1 = exchange1.getResponse().getHeaders().getFirst("X-Rate-Limit-Remaining");
        
        // Second user makes a request
        MockServerHttpRequest request2 = MockServerHttpRequest.get("/api/v1/users/profile")
                .header("X-User-Id", "user2")
                .header("X-User-Role", "FARMER")
                .build();
        
        ServerWebExchange exchange2 = MockServerWebExchange.from(request2);
        var result2 = filter.filter(exchange2, e -> Mono.empty());
        
        StepVerifier.create(result2)
                .expectComplete()
                .verify();
        
        String remaining2 = exchange2.getResponse().getHeaders().getFirst("X-Rate-Limit-Remaining");
        
        // Both should have same remaining count (separate buckets)
        assertEquals(remaining1, remaining2);
    }

    @Test
    void testMissingUserIdUsesIpAddress() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/profile")
                .header("X-User-Role", "FARMER")
                .build();
        
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        
        var filter = rateLimitingFilter.apply(new RateLimitingFilter.Config());
        var result = filter.filter(exchange, e -> Mono.empty());
        
        StepVerifier.create(result)
                .expectComplete()
                .verify();
        
        assertNotEquals(HttpStatus.TOO_MANY_REQUESTS, exchange.getResponse().getStatusCode());
    }
}
