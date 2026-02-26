package com.farmer.yield.client;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;


import java.util.HashMap;
import java.util.Map;

/**
 * Client for fetching mandi price data.
 * 
 * Used for:
 * - Current commodity prices
 * - Price trends
 * - MSP comparison
 * 
 * Validates: Requirement 11B.10
 */
@Component
public class MandiPriceClient {

    private final WebClient mandiServiceClient;

    public MandiPriceClient(WebClient.Builder webClientBuilder) {
        this.mandiServiceClient = webClientBuilder
                .baseUrl("http://mandi-service:8080")
                .build();
    }

    /**
     * Get current price for a commodity.
     * 
     * @param commodityName Commodity name (e.g., "RICE", "WHEAT")
     * @return Price data including modal, min, max prices
     */
    public Map<String, Double> getCurrentPrice(String commodityName) {
        try {
            Map<String, Object> response = mandiServiceClient.get()
                    .uri("/api/v1/mandi/prices/{commodity}", commodityName)
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            Map<String, Double> priceData = new HashMap<>();
            if (response != null && response.containsKey("prices")) {
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> prices = (java.util.List<Map<String, Object>>) response.get("prices");
                if (!prices.isEmpty()) {
                    Map<String, Object> latestPrice = prices.get(0);
                    priceData.put("modalPrice", new Double(latestPrice.getOrDefault("modalPrice", "2000").toString()));
                    priceData.put("minPrice", new Double(latestPrice.getOrDefault("minPrice", "1800").toString()));
                    priceData.put("maxPrice", new Double(latestPrice.getOrDefault("maxPrice", "2200").toString()));
                }
            }
            return priceData;
        } catch (Exception e) {
            // Return default prices on failure
            Map<String, Double> defaultPrices = new HashMap<>();
            defaultPrices.put("modalPrice", new Double("2000"));
            defaultPrices.put("minPrice", new Double("1800"));
            defaultPrices.put("maxPrice", new Double("2200"));
            return defaultPrices;
        }
    }

    /**
     * Get price trends for a commodity.
     * 
     * @param commodityName Commodity name
     * @return Price trend data for the past 30 days
     */
    public Map<String, Object> getPriceTrends(String commodityName) {
        try {
            return mandiServiceClient.get()
                    .uri("/api/v1/mandi/prices/trends/{commodity}", commodityName)
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Get MSP for a commodity.
     * 
     * @param commodityName Commodity name
     * @return MSP data
     */
    public Map<String, Object> getMspPrice(String commodityName) {
        try {
            return mandiServiceClient.get()
                    .uri("/api/v1/mandi/prices/msp/{commodity}", commodityName)
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Get nearby mandis with prices.
     * 
     * @param commodityName Commodity name
     * @param latitude GPS latitude
     * @param longitude GPS longitude
     * @return List of nearby mandis with prices
     */
    public Map<String, Object> getNearbyMandis(String commodityName, Double latitude, Double longitude) {
        try {
            return mandiServiceClient.get()
                    .uri("/api/v1/mandi/prices/nearby?commodity={commodity}&lat={lat}&lng={lng}", 
                            commodityName, latitude, longitude)
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}