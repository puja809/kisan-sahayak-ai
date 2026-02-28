package com.farmer.yield.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.farmer.yield.entity.YieldCalculator;
import com.farmer.yield.repository.YieldCalculatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Data loader to populate YieldCalculator table from ultimate_calculator_app.json
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class YieldCalculatorDataLoader implements CommandLineRunner {

    private final YieldCalculatorRepository yieldCalculatorRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) throws Exception {
        if (yieldCalculatorRepository.count() > 0) {
            log.info("YieldCalculator data already loaded, skipping initialization");
            return;
        }

        log.info("Loading yield calculator data from JSON file");
        
        try {
            ClassPathResource resource = new ClassPathResource("ultimate_calculator_app.json");
            InputStream inputStream = resource.getInputStream();
            JsonNode rootNode = objectMapper.readTree(inputStream);

            if (rootNode.has("price_stats")) {
                JsonNode priceStats = rootNode.get("price_stats");
                
                for (JsonNode stat : priceStats) {
                    String commodity = stat.get("Commodity").asText();
                    Double minPrice = stat.get("Min_Price_kg").asDouble();
                    Double avgPrice = stat.get("Avg_Price_kg").asDouble();
                    Double maxPrice = stat.get("Max_Price_kg").asDouble();

                    // Base yield per hectare (in kg) - using average price as proxy for yield estimation
                    Double baseYield = avgPrice * 50; // Approximate base yield

                    YieldCalculator calculator = YieldCalculator.builder()
                            .commodity(commodity)
                            .minPricePerKg(minPrice)
                            .avgPricePerKg(avgPrice)
                            .maxPricePerKg(maxPrice)
                            .baseYieldPerHectare(baseYield)
                            .yieldVariancePercent(15.0) // Default 15% variance
                            .isActive(true)
                            .build();

                    yieldCalculatorRepository.save(calculator);
                }

                log.info("Successfully loaded {} yield calculator records", priceStats.size());
            }
        } catch (Exception e) {
            log.error("Error loading yield calculator data: {}", e.getMessage(), e);
        }
    }
}
