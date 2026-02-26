package com.farmer.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Request/Response Transformation Filter
 * Adds common headers and transforms requests/responses
 */
@Component
public class RequestResponseTransformationFilter extends AbstractGatewayFilterFactory<RequestResponseTransformationFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(RequestResponseTransformationFilter.class);

    public RequestResponseTransformationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // Add request tracking headers
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-Request-Id", generateRequestId())
                    .header("X-Request-Timestamp", String.valueOf(System.currentTimeMillis()))
                    .header("X-Forwarded-For", getClientIp(request))
                    .build();

            ServerWebExchange modifiedExchange = exchange.mutate()
                    .request(modifiedRequest)
                    .build();

            // Add response headers
            modifiedExchange.getResponse().getHeaders().add("X-Response-Time", String.valueOf(System.currentTimeMillis()));
            modifiedExchange.getResponse().getHeaders().add("X-API-Version", "v1");

            logger.debug("Request transformed: {} {}", request.getMethod(), request.getURI().getPath());
            return chain.filter(modifiedExchange);
        };
    }

    /**
     * Generate unique request ID for tracking
     */
    private String generateRequestId() {
        return "REQ-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId();
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        
        return "unknown";
    }

    public static class Config {
        // Configuration class for filter
    }
}
