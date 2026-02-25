package com.farmer.weather.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Configuration for IMD API client.
 * Sets up WebClient with appropriate timeouts and connection settings.
 */
@Configuration
public class ImdApiConfig {

    @Value("${imd.api.base-url:https://api.imd.gov.in}")
    private String imdApiBaseUrl;

    @Value("${imd.api.key:}")
    private String imdApiKey;

    @Value("${imd.api.timeout-seconds:30}")
    private int timeoutSeconds;

    @Value("${imd.api.rate-limit-requests-per-minute:60}")
    private int rateLimitRequestsPerMinute;

    @Bean
    public WebClient imdWebClient() {
        // Configure connection pool and timeouts
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(timeoutSeconds))
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutSeconds * 1000);

        // Configure strategies to avoid memory issues with large responses
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();

        WebClient.Builder builder = WebClient.builder()
                .baseUrl(imdApiBaseUrl)
                .exchangeStrategies(strategies)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Content-Type", "application/json");

        if (imdApiKey != null && !imdApiKey.isEmpty()) {
            builder.defaultHeader("Authorization", "Bearer " + imdApiKey);
        }

        return builder.build();
    }

    @Bean
    public ImdApiProperties imdApiProperties() {
        return new ImdApiProperties(imdApiBaseUrl, imdApiKey, timeoutSeconds, rateLimitRequestsPerMinute);
    }
}

/**
 * Properties for IMD API configuration.
 */
class ImdApiProperties {
    private final String baseUrl;
    private final String apiKey;
    private final int timeoutSeconds;
    private final int rateLimitRequestsPerMinute;

    public ImdApiProperties(String baseUrl, String apiKey, int timeoutSeconds, int rateLimitRequestsPerMinute) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.timeoutSeconds = timeoutSeconds;
        this.rateLimitRequestsPerMinute = rateLimitRequestsPerMinute;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public int getRateLimitRequestsPerMinute() {
        return rateLimitRequestsPerMinute;
    }
}