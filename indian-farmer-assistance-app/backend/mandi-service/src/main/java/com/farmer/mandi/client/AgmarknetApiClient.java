package com.farmer.mandi.client;

import com.farmer.mandi.dto.MandiPriceDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * AGMARKNET API client for fetching agricultural market price data.
 * 
 * Requirements:
 * - 6.1: Retrieve current prices from AGMARKNET API
 * - 6.2: Display modal price, min price, max price, arrival quantity, variety
 * - 6.3: Access near real-time data recorded by regulated market staff
 */
@Component
@Slf4j
public class AgmarknetApiClient {

    private final WebClient webClient;
    private final int connectionTimeoutSeconds;
    private final int readTimeoutSeconds;
    private final int maxRetries;

    public AgmarknetApiClient(
            WebClient.Builder webClientBuilder,
            @Value("${agmarknet.api.base-url:https://agmarknet.gov.in}") String baseUrl,
            @Value("${agmarknet.api.connection-timeout:30}") int connectionTimeoutSeconds,
            @Value("${agmarknet.api.read-timeout:60}") int readTimeoutSeconds,
            @Value("${agmarknet.api.max-retries:3}") int maxRetries) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.connectionTimeoutSeconds = connectionTimeoutSeconds;
        this.readTimeoutSeconds = readTimeoutSeconds;
        this.maxRetries = maxRetries;
    }

    /**
     * Fetches current prices for a commodity from AGMARKNET.
     * 
     * @param commodity The commodity name (e.g., "Paddy", "Wheat")
     * @return List of MandiPriceDto with current prices
     */
    public Mono<List<MandiPriceDto>> getCommodityPrices(String commodity) {
        log.info("Fetching prices for commodity: {}", commodity);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/commodityPrices")
                        .queryParam("commodity", commodity)
                        .build())
                .retrieve()
                .bodyToMono(AgmarknetPriceResponse.class)
                .timeout(Duration.ofSeconds(readTimeoutSeconds))
                .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(1))
                        .filter(this::isRetryableException)
                        .doBeforeRetry(signal -> log.warn("Retrying AGMARKNET API call, attempt {}", signal.totalRetries() + 1)))
                .map(response -> mapToPriceDtos(response, commodity))
                .onErrorResume(this::handleError);
    }

    /**
     * Fetches prices for a commodity in a specific state.
     * 
     * @param commodity The commodity name
     * @param state The state name
     * @return List of MandiPriceDto with prices
     */
    public Mono<List<MandiPriceDto>> getCommodityPricesByState(String commodity, String state) {
        log.info("Fetching prices for commodity: {} in state: {}", commodity, state);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/commodityPrices")
                        .queryParam("commodity", commodity)
                        .queryParam("state", state)
                        .build())
                .retrieve()
                .bodyToMono(AgmarknetPriceResponse.class)
                .timeout(Duration.ofSeconds(readTimeoutSeconds))
                .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(1))
                        .filter(this::isRetryableException)
                        .doBeforeRetry(signal -> log.warn("Retrying AGMARKNET API call, attempt {}", signal.totalRetries() + 1)))
                .map(response -> mapToPriceDtos(response, commodity))
                .onErrorResume(this::handleError);
    }

    /**
     * Fetches prices for a commodity in a specific district.
     * 
     * @param commodity The commodity name
     * @param state The state name
     * @param district The district name
     * @return List of MandiPriceDto with prices
     */
    public Mono<List<MandiPriceDto>> getCommodityPricesByDistrict(String commodity, String state, String district) {
        log.info("Fetching prices for commodity: {} in district: {}", commodity, district);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/commodityPrices")
                        .queryParam("commodity", commodity)
                        .queryParam("state", state)
                        .queryParam("district", district)
                        .build())
                .retrieve()
                .bodyToMono(AgmarknetPriceResponse.class)
                .timeout(Duration.ofSeconds(readTimeoutSeconds))
                .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(1))
                        .filter(this::isRetryableException)
                        .doBeforeRetry(signal -> log.warn("Retrying AGMARKNET API call, attempt {}", signal.totalRetries() + 1)))
                .map(response -> mapToPriceDtos(response, commodity))
                .onErrorResume(this::handleError);
    }

    /**
     * Fetches historical prices for a commodity for trend analysis.
     * 
     * @param commodity The commodity name
     * @param days Number of days of historical data
     * @return List of MandiPriceDto with historical prices
     */
    public Mono<List<MandiPriceDto>> getHistoricalPrices(String commodity, int days) {
        log.info("Fetching {} days of historical prices for commodity: {}", days, commodity);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/historicalPrices")
                        .queryParam("commodity", commodity)
                        .queryParam("days", days)
                        .build())
                .retrieve()
                .bodyToMono(AgmarknetPriceResponse.class)
                .timeout(Duration.ofSeconds(readTimeoutSeconds))
                .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(1))
                        .filter(this::isRetryableException)
                        .doBeforeRetry(signal -> log.warn("Retrying AGMARKNET API call, attempt {}", signal.totalRetries() + 1)))
                .map(response -> mapToPriceDtos(response, commodity))
                .onErrorResume(this::handleError);
    }

    /**
     * Fetches all available commodities from AGMARKNET.
     * 
     * @return List of commodity names
     */
    public Mono<List<String>> getCommodities() {
        log.info("Fetching list of commodities from AGMARKNET");
        
        return webClient.get()
                .uri("/api/commodities")
                .retrieve()
                .bodyToMono(AgmarknetCommoditiesResponse.class)
                .map(AgmarknetCommoditiesResponse::getCommodities)
                .onErrorResume(this::handleError);
    }

    /**
     * Checks if the AGMARKNET API is available.
     * 
     * @return true if API is available, false otherwise
     */
    public Mono<Boolean> isApiAvailable() {
        return webClient.get()
                .uri("/api/health")
                .retrieve()
                .bodyToMono(Void.class)
                .map(response -> true)
                .onErrorResume(error -> {
                    log.error("AGMARKNET API health check failed: {}", error.getMessage());
                    return Mono.just(false);
                });
    }

    private boolean isRetryableException(Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            return ex.getStatusCode().is5xxServerError();
        }
        if (throwable instanceof java.net.SocketTimeoutException ||
            throwable instanceof java.net.ConnectException) {
            return true;
        }
        return false;
    }

    private <T> Mono<T> handleError(Throwable error) {
        if (error instanceof WebClientResponseException ex) {
            log.error("AGMARKNET API error: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Mono.empty();
            }
        } else {
            log.error("Error fetching from AGMARKNET: {}", error.getMessage());
        }
        return Mono.error(new RuntimeException("Failed to fetch data from AGMARKNET: " + error.getMessage(), error));
    }

    private List<MandiPriceDto> mapToPriceDtos(AgmarknetPriceResponse response, String commodity) {
        List<MandiPriceDto> priceDtos = new ArrayList<>();
        
        if (response == null || response.getData() == null) {
            return priceDtos;
        }

        for (AgmarknetPriceData data : response.getData()) {
            MandiPriceDto dto = MandiPriceDto.builder()
                    .commodityName(commodity)
                    .variety(data.getVariety())
                    .mandiName(data.getMandi())
                    .mandiCode(data.getMandiCode())
                    .state(data.getState())
                    .district(data.getDistrict())
                    .priceDate(data.getDate() != null ? data.getDate().toLocalDate() : LocalDate.now())
                    .modalPrice(data.getModalPrice())
                    .minPrice(data.getMinPrice())
                    .maxPrice(data.getMaxPrice())
                    .arrivalQuantityQuintals(data.getArrivalQuantity())
                    .unit(data.getUnit() != null ? data.getUnit() : "Quintal")
                    .source("AGMARKNET")
                    .isCached(false)
                    .build();
            priceDtos.add(dto);
        }

        return priceDtos;
    }

    // Inner classes for AGMARKNET API response mapping
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AgmarknetPriceResponse {
        private List<AgmarknetPriceData> data;
        private boolean success;
        private String message;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AgmarknetPriceData {
        private String commodity;
        private String variety;
        private String mandi;
        private String mandiCode;
        private String state;
        private String district;
        private java.time.LocalDate date;
        private BigDecimal modalPrice;
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private BigDecimal arrivalQuantity;
        private String unit;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AgmarknetCommoditiesResponse {
        private List<String> commodities;
        private boolean success;
    }
}