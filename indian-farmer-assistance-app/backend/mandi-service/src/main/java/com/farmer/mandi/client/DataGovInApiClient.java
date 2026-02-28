package com.farmer.mandi.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * API client for data.gov.in endpoints.
 * 
 * Integrates with government APIs for mandi prices and fertilizer suppliers.
 * Implements retry logic, caching, and error handling.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataGovInApiClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${api.datagov.mandi-price.url:https://api.data.gov.in/resource/9ef84268-d588-465a-a308-a864a43d0070}")
    private String mandiPriceUrl;

    @Value("${api.datagov.mandi-price.api-key}")
    private String mandiPriceApiKey;

    @Value("${api.datagov.fertilizer-supplier.url:https://api.data.gov.in/resource/56f40018-fd03-4010-94a3-f34ca7b43f7c}")
    private String fertilizerSupplierUrl;

    @Value("${api.datagov.fertilizer-supplier.api-key}")
    private String fertilizerSupplierApiKey;

    @Value("${api.datagov.timeout:10000}")
    private long apiTimeout;

    @Value("${api.datagov.retry.max-attempts:3}")
    private long maxRetries;

    /**
     * Get mandi prices for a commodity with optional filters.
     * 
     * @param state State name (optional)
     * @param district District name (optional)
     * @param market Market/Mandi name (optional)
     * @param commodity Commodity name (optional)
     * @param offset Pagination offset
     * @param limit Results per page
     * @return List of mandi price records
     */
    public Mono<List<MandiPriceRecord>> getMandiPrices(
            String state,
            String district,
            String market,
            String commodity,
            int offset,
            int limit) {

        log.info("Fetching mandi prices: state={}, district={}, market={}, commodity={}", 
                 state, district, market, commodity);

        StringBuilder urlBuilder = new StringBuilder(mandiPriceUrl);
        urlBuilder.append("?api-key=").append(mandiPriceApiKey);
        urlBuilder.append("&format=json");
        urlBuilder.append("&offset=").append(offset);
        urlBuilder.append("&limit=").append(Math.min(limit, 100)); // Max 100 per request

        if (state != null && !state.isEmpty()) {
            urlBuilder.append("&filters[state.keyword]=").append(encodeParam(state));
        }
        if (district != null && !district.isEmpty()) {
            urlBuilder.append("&filters[district]=").append(encodeParam(district));
        }
        if (market != null && !market.isEmpty()) {
            urlBuilder.append("&filters[market]=").append(encodeParam(market));
        }
        if (commodity != null && !commodity.isEmpty()) {
            urlBuilder.append("&filters[commodity]=").append(encodeParam(commodity));
        }

        return webClient.get()
                .uri(urlBuilder.toString())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(apiTimeout))
                .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(1))
                        .filter(this::isRetryableException))
                .map(this::parseMandiPriceResponse)
                .doOnError(error -> log.error("Error fetching mandi prices: {}", error.getMessage()))
                .onErrorReturn(new ArrayList<>());
    }

    /**
     * Get fertilizer supplier information with optional filters.
     * 
     * @param state State name (optional)
     * @param district District name (optional)
     * @param offset Pagination offset
     * @param limit Results per page
     * @return List of fertilizer supplier records
     */
    public Mono<List<FertilizerSupplierRecord>> getFertilizerSuppliers(
            String state,
            String district,
            int offset,
            int limit) {

        log.info("Fetching fertilizer suppliers: state={}, district={}", state, district);

        StringBuilder urlBuilder = new StringBuilder(fertilizerSupplierUrl);
        urlBuilder.append("?api-key=").append(fertilizerSupplierApiKey);
        urlBuilder.append("&format=json");
        urlBuilder.append("&offset=").append(offset);
        urlBuilder.append("&limit=").append(Math.min(limit, 100)); // Max 100 per request

        if (state != null && !state.isEmpty()) {
            urlBuilder.append("&filters[state]=").append(encodeParam(state));
        }
        if (district != null && !district.isEmpty()) {
            urlBuilder.append("&filters[district]=").append(encodeParam(district));
        }

        return webClient.get()
                .uri(urlBuilder.toString())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(apiTimeout))
                .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(1))
                        .filter(this::isRetryableException))
                .map(this::parseFertilizerSupplierResponse)
                .doOnError(error -> log.error("Error fetching fertilizer suppliers: {}", error.getMessage()))
                .onErrorReturn(new ArrayList<>());
    }

    /**
     * Check if API is available.
     * 
     * @return True if API is reachable
     */
    public Mono<Boolean> isApiAvailable() {
        return webClient.get()
                .uri(mandiPriceUrl + "?api-key=" + mandiPriceApiKey + "&format=json&limit=1")
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofMillis(apiTimeout))
                .map(response -> response.getStatusCode().is2xxSuccessful())
                .onErrorReturn(false);
    }

    /**
     * Parse mandi price response from API.
     * 
     * @param response JSON response string
     * @return List of mandi price records
     */
    private List<MandiPriceRecord> parseMandiPriceResponse(String response) {
        List<MandiPriceRecord> records = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode recordsNode = root.get("records");
            
            if (recordsNode != null && recordsNode.isArray()) {
                recordsNode.forEach(record -> {
                    MandiPriceRecord priceRecord = MandiPriceRecord.builder()
                            .state(getStringValue(record, "state"))
                            .district(getStringValue(record, "district"))
                            .market(getStringValue(record, "market"))
                            .commodity(getStringValue(record, "commodity"))
                            .variety(getStringValue(record, "variety"))
                            .arrivalDate(getStringValue(record, "arrival_date"))
                            .minPrice(getDoubleValue(record, "min_price"))
                            .maxPrice(getDoubleValue(record, "max_price"))
                            .modalPrice(getDoubleValue(record, "modal_price"))
                            .arrivalQuantity(getDoubleValue(record, "arrival_quantity"))
                            .unit(getStringValue(record, "unit"))
                            .build();
                    records.add(priceRecord);
                });
            }
        } catch (Exception e) {
            log.error("Error parsing mandi price response: {}", e.getMessage());
        }
        return records;
    }

    /**
     * Parse fertilizer supplier response from API.
     * 
     * @param response JSON response string
     * @return List of fertilizer supplier records
     */
    private List<FertilizerSupplierRecord> parseFertilizerSupplierResponse(String response) {
        List<FertilizerSupplierRecord> records = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode recordsNode = root.get("records");
            
            if (recordsNode != null && recordsNode.isArray()) {
                recordsNode.forEach(record -> {
                    FertilizerSupplierRecord supplierRecord = FertilizerSupplierRecord.builder()
                            .state(getStringValue(record, "state"))
                            .district(getStringValue(record, "district"))
                            .documentId(getStringValue(record, "document_id"))
                            .slNo(getIntValue(record, "sl_no"))
                            .noOfWholesalers(getIntValue(record, "no_of_wholesalers"))
                            .noOfRetailers(getIntValue(record, "no_of_retailers"))
                            .fertilizerType(getStringValue(record, "fertilizer_type"))
                            .supplierName(getStringValue(record, "supplier_name"))
                            .contactInfo(getStringValue(record, "contact_info"))
                            .build();
                    records.add(supplierRecord);
                });
            }
        } catch (Exception e) {
            log.error("Error parsing fertilizer supplier response: {}", e.getMessage());
        }
        return records;
    }

    /**
     * Check if exception is retryable.
     * 
     * @param throwable Exception to check
     * @return True if exception is retryable
     */
    private boolean isRetryableException(Throwable throwable) {
        return throwable instanceof java.net.ConnectException ||
               throwable instanceof java.net.SocketTimeoutException ||
               throwable instanceof java.io.IOException;
    }

    /**
     * Encode parameter for URL.
     * 
     * @param param Parameter to encode
     * @return Encoded parameter
     */
    private String encodeParam(String param) {
        try {
            return java.net.URLEncoder.encode(param, "UTF-8");
        } catch (Exception e) {
            return param;
        }
    }

    /**
     * Get string value from JSON node.
     * 
     * @param node JSON node
     * @param fieldName Field name
     * @return String value or empty string
     */
    private String getStringValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field != null ? field.asText("") : "";
    }

    /**
     * Get double value from JSON node.
     * 
     * @param node JSON node
     * @param fieldName Field name
     * @return Double value or 0.0
     */
    private Double getDoubleValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field != null ? field.asDouble(0.0) : 0.0;
    }

    /**
     * Get integer value from JSON node.
     * 
     * @param node JSON node
     * @param fieldName Field name
     * @return Integer value or 0
     */
    private Integer getIntValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field != null ? field.asInt(0) : 0;
    }

    /**
     * Mandi price record from data.gov.in API.
     */
    @lombok.Data
    @lombok.Builder
    public static class MandiPriceRecord {
        private String state;
        private String district;
        private String market;
        private String commodity;
        private String variety;
        private String arrivalDate;
        private Double minPrice;
        private Double maxPrice;
        private Double modalPrice;
        private Double arrivalQuantity;
        private String unit;
    }

    /**
     * Fertilizer supplier record from data.gov.in API.
     */
    @lombok.Data
    @lombok.Builder
    public static class FertilizerSupplierRecord {
        private String state;
        private String district;
        private String documentId;
        private Integer slNo;
        private Integer noOfWholesalers;
        private Integer noOfRetailers;
        private String fertilizerType;
        private String supplierName;
        private String contactInfo;
    }
}
