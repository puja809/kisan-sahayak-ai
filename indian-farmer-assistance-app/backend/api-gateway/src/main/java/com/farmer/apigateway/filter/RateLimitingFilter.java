package com.farmer.apigateway.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Filter
 * Implements rate limiting per user and endpoint using token bucket algorithm
 */
@Component
public class RateLimitingFilter extends AbstractGatewayFilterFactory<RateLimitingFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);
    private static final String X_USER_ID_HEADER = "X-User-Id";
    private static final String X_RATE_LIMIT_REMAINING = "X-Rate-Limit-Remaining";
    private static final String X_RATE_LIMIT_RESET = "X-Rate-Limit-Reset";

    // Default rate limits
    private static final int DEFAULT_REQUESTS_PER_MINUTE = 100;
    private static final int ADMIN_REQUESTS_PER_MINUTE = 500;

    // Store buckets per user
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitingFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String userId = exchange.getRequest().getHeaders().getFirst(X_USER_ID_HEADER);
            String userRole = exchange.getRequest().getHeaders().getFirst("X-User-Role");

            if (userId == null || userId.isEmpty()) {
                userId = exchange.getRequest().getRemoteAddress() != null
                        ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                        : "unknown";
            }

            // Get or create bucket for this user
            Bucket bucket = getBucket(userId, userRole);

            // Try to consume a token
            if (bucket.tryConsume(1)) {
                long tokensRemaining = bucket.getAvailableTokens();
                exchange.getResponse().getHeaders().add(X_RATE_LIMIT_REMAINING, String.valueOf(tokensRemaining));
                logger.debug("Rate limit check passed for user: {}, remaining: {}", userId, tokensRemaining);
                return chain.filter(exchange);
            } else {
                // Rate limit exceeded
                logger.warn("Rate limit exceeded for user: {}", userId);
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                exchange.getResponse().getHeaders().add(X_RATE_LIMIT_RESET, 
                        String.valueOf(System.currentTimeMillis() + 60000));
                return exchange.getResponse().setComplete();
            }
        };
    }

    /**
     * Get or create a bucket for the user
     * Different limits for ADMIN vs FARMER roles
     */
    private Bucket getBucket(String userId, String userRole) {
        return buckets.computeIfAbsent(userId, key -> {
            int requestsPerMinute = "ADMIN".equals(userRole) 
                    ? ADMIN_REQUESTS_PER_MINUTE 
                    : DEFAULT_REQUESTS_PER_MINUTE;
            
            Bandwidth limit = Bandwidth.classic(requestsPerMinute, Refill.intervally(requestsPerMinute, Duration.ofMinutes(1)));
            return Bucket4j.builder()
                    .addLimit(limit)
                    .build();
        });
    }

    public static class Config {
        // Configuration class for filter
    }
}
