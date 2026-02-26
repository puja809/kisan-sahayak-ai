package com.farmer.apigateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationFilterTest {

    private AuthenticationFilter authenticationFilter;
    private String jwtSecret = "test-secret-key-that-is-long-enough-for-hs256";
    private long jwtExpiration = 86400000;

    @BeforeEach
    void setUp() {
        authenticationFilter = new AuthenticationFilter();
        ReflectionTestUtils.setField(authenticationFilter, "jwtSecret", jwtSecret);
        ReflectionTestUtils.setField(authenticationFilter, "jwtExpiration", jwtExpiration);
    }

    @Test
    void testValidTokenAllowsRequest() {
        // Generate valid JWT token
        String token = generateValidToken("user123", "FARMER", "user@example.com");
        
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + token)
                .build();
        
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        
        // Create a simple chain that returns success
        var filter = authenticationFilter.apply(new AuthenticationFilter.Config());
        var result = filter.filter(exchange, e -> Mono.empty());
        
        StepVerifier.create(result)
                .expectComplete()
                .verify();
        
        // Verify user context headers were added
        assertEquals("user123", exchange.getRequest().getHeaders().getFirst("X-User-Id"));
        assertEquals("FARMER", exchange.getRequest().getHeaders().getFirst("X-User-Role"));
    }

    @Test
    void testMissingAuthorizationHeaderReturnsUnauthorized() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/profile")
                .build();
        
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        
        var filter = authenticationFilter.apply(new AuthenticationFilter.Config());
        var result = filter.filter(exchange, e -> Mono.empty());
        
        StepVerifier.create(result)
                .expectComplete()
                .verify();
        
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void testInvalidTokenReturnsUnauthorized() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/profile")
                .header("Authorization", "Bearer invalid-token")
                .build();
        
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        
        var filter = authenticationFilter.apply(new AuthenticationFilter.Config());
        var result = filter.filter(exchange, e -> Mono.empty());
        
        StepVerifier.create(result)
                .expectComplete()
                .verify();
        
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void testPublicEndpointSkipsAuthentication() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/users/login")
                .build();
        
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        
        var filter = authenticationFilter.apply(new AuthenticationFilter.Config());
        var result = filter.filter(exchange, e -> Mono.empty());
        
        StepVerifier.create(result)
                .expectComplete()
                .verify();
        
        // Should not return unauthorized for public endpoint
        assertNotEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void testAdminRoleIsExtractedFromToken() {
        String token = generateValidToken("admin123", "ADMIN", "admin@example.com");
        
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/admin/users")
                .header("Authorization", "Bearer " + token)
                .build();
        
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        
        var filter = authenticationFilter.apply(new AuthenticationFilter.Config());
        var result = filter.filter(exchange, e -> Mono.empty());
        
        StepVerifier.create(result)
                .expectComplete()
                .verify();
        
        assertEquals("admin123", exchange.getRequest().getHeaders().getFirst("X-User-Id"));
        assertEquals("ADMIN", exchange.getRequest().getHeaders().getFirst("X-User-Role"));
    }

    private String generateValidToken(String userId, String role, String email) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject(userId)
                .claim("role", role)
                .claim("email", email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(key)
                .compact();
    }
}
