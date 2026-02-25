package com.farmer.user.service;

import com.farmer.user.dto.AgriStackProfileResponse;
import com.farmer.user.dto.AgriStackProfileResponse.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Service for AgriStack UFSI (Unified Farmer Service Interface) integration.
 * Handles farmer authentication and profile retrieval from the three core registries:
 * - Farmer Registry
 * - Geo-Referenced Village Map Registry
 * - Crop Sown Registry
 * 
 * Requirements: 11.1, 11.2, 11.3, 11.9
 */
@Service
@Slf4j
public class AgriStackService {

    @Value("${app.agristack.base-url}")
    private String baseUrl;

    @Value("${app.agristack.ufsi-endpoint}")
    private String ufsiEndpoint;

    @Value("${app.agristack.farmer-registry-endpoint}")
    private String farmerRegistryEndpoint;

    @Value("${app.agristack.geo-map-registry-endpoint}")
    private String geoMapRegistryEndpoint;

    @Value("${app.agristack.crop-sown-registry-endpoint}")
    private String cropSownRegistryEndpoint;

    @Value("${app.agristack.timeout:30000}")
    private int timeout;

    private final WebClient webClient;

    public AgriStackService() {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Authenticate farmer using AgriStack UFSI.
     * Requirements: 11.1, 11.2
     */
    public Mono<AgriStackAuthResponse> authenticateFarmer(String aadhaarNumber, String phoneNumber) {
        log.info("Authenticating farmer with AgriStack UFSI: phone={}", phoneNumber);

        Map<String, Object> requestBody = Map.of(
                "aadhaarNumber", aadhaarNumber,
                "phoneNumber", phoneNumber,
                "authenticationMode", "OTP"
        );

        return webClient.post()
                .uri(ufsiEndpoint + "/authenticate")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new AgriStackException(
                                        "Authentication failed: " + clientResponse.statusCode(),
                                        body))))
                .bodyToMono(AgriStackAuthResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(this::isRetryableError))
                .timeout(Duration.ofMillis(timeout))
                .doOnSuccess(response -> log.info("AgriStack authentication successful for: {}", phoneNumber))
                .doOnError(error -> log.error("AgriStack authentication failed: {}", error.getMessage()));
    }

    /**
     * Retrieve farmer profile from all three core registries.
     * Requirements: 11.2, 11.3
     */
    public Mono<AgriStackProfileResponse> getFarmerProfile(String agristackFarmerId) {
        log.info("Retrieving AgriStack profile for farmer: {}", agristackFarmerId);

        Mono<FarmerRegistryInfo> farmerRegistryMono = getFarmerRegistry(agristackFarmerId)
                .onErrorResume(e -> {
                    log.warn("Failed to retrieve farmer registry: {}", e.getMessage());
                    return Mono.empty();
                });

        Mono<GeoMapRegistryInfo> geoMapRegistryMono = getGeoMapRegistry(agristackFarmerId)
                .onErrorResume(e -> {
                    log.warn("Failed to retrieve geo map registry: {}", e.getMessage());
                    return Mono.empty();
                });

        Mono<CropSownRegistryInfo> cropSownRegistryMono = getCropSownRegistry(agristackFarmerId)
                .onErrorResume(e -> {
                    log.warn("Failed to retrieve crop sown registry: {}", e.getMessage());
                    return Mono.empty();
                });

        return Mono.zip(farmerRegistryMono, geoMapRegistryMono, cropSownRegistryMono)
                .map(tuple -> AgriStackProfileResponse.builder()
                        .farmerRegistry(tuple.getT1())
                        .geoMapRegistry(tuple.getT2())
                        .cropSownRegistry(tuple.getT3())
                        .success(true)
                        .retrievedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .build())
                .defaultIfEmpty(AgriStackProfileResponse.builder()
                        .success(false)
                        .errorMessage("Failed to retrieve profile from all registries")
                        .retrievedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .build())
                .doOnSuccess(response -> log.info("AgriStack profile retrieval completed for: {}", agristackFarmerId))
                .doOnError(error -> log.error("AgriStack profile retrieval failed: {}", error.getMessage()));
    }

    /**
     * Get farmer registry information.
     * Requirements: 11.2
     */
    private Mono<FarmerRegistryInfo> getFarmerRegistry(String farmerId) {
        return webClient.get()
                .uri(farmerRegistryEndpoint + "/farmers/" + farmerId)
                .retrieve()
                .bodyToMono(FarmerRegistryInfo.class)
                .timeout(Duration.ofMillis(timeout));
    }

    /**
     * Get geo-referenced village map registry information.
     * Requirements: 11.3
     */
    private Mono<GeoMapRegistryInfo> getGeoMapRegistry(String farmerId) {
        return webClient.get()
                .uri(geoMapRegistryEndpoint + "/farmers/" + farmerId + "/land-parcels")
                .retrieve()
                .bodyToMono(GeoMapRegistryInfo.class)
                .timeout(Duration.ofMillis(timeout));
    }

    /**
     * Get crop sown registry information.
     * Requirements: 11.3
     */
    private Mono<CropSownRegistryInfo> getCropSownRegistry(String farmerId) {
        return webClient.get()
                .uri(cropSownRegistryEndpoint + "/farmers/" + farmerId + "/crops")
                .retrieve()
                .bodyToMono(CropSownRegistryInfo.class)
                .timeout(Duration.ofMillis(timeout));
    }

    /**
     * Check if an error is retryable.
     */
    private boolean isRetryableError(Throwable error) {
        if (error instanceof WebClientResponseException e) {
            int statusCode = e.getStatusCode().value();
            // Retry on server errors (5xx) and rate limiting (429)
            return statusCode >= 500 || statusCode == 429;
        }
        return false;
    }

    /**
     * Check if AgriStack service is available.
     * Requirements: 11.9
     */
    public Mono<Boolean> isServiceAvailable() {
        return webClient.get()
                .uri(ufsiEndpoint + "/health")
                .retrieve()
                .toBodilessEntity()
                .map(response -> true)
                .onErrorResume(e -> {
                    log.warn("AgriStack service unavailable: {}", e.getMessage());
                    return Mono.just(false);
                })
                .timeout(Duration.ofSeconds(5));
    }

    /**
     * Response from AgriStack authentication.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AgriStackAuthResponse {
        private String farmerId;
        private String status;
        private String message;
        private Boolean isAuthenticated;
    }

    /**
     * Custom exception for AgriStack service errors.
     */
    public static class AgriStackException extends RuntimeException {
        private final String responseBody;

        public AgriStackException(String message, String responseBody) {
            super(message);
            this.responseBody = responseBody;
        }

        public String getResponseBody() {
            return responseBody;
        }
    }
}