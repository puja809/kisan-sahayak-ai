package com.farmer.mandi.service;

import com.farmer.mandi.client.AgmarknetApiClient;
import com.farmer.mandi.dto.MandiPriceDto;
import com.farmer.mandi.entity.MandiPrices;
import com.farmer.mandi.repository.MandiPricesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for managing mandi price data with AGMARKNET integration and caching.
 * 
 * Requirements:
 * - 6.1: Retrieve current prices from AGMARKNET API
 * - 6.2: Display modal price, min price, max price, arrival quantity, variety
 * - 6.11: Display cached prices with timestamp when AGMARKNET unavailable
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MandiPriceService {

    private final AgmarknetApiClient agmarknetApiClient;
    private final MandiPricesRepository mandiPricesRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${redis.cache.mandi-price-ttl-hours:1}")
    private int cacheTtlHours;

    private static final String CACHE_KEY_PREFIX = "mandi:price:";
    private static final String CACHE_KEY_COMMODITY = "commodity:";
    private static final String CACHE_KEY_HISTORY = "history:";

    /**
     * Fetches current prices for a commodity from AGMARKNET or cache.
     * 
     * @param commodity The commodity name
     * @return List of MandiPriceDto with current prices
     */
    public Mono<List<MandiPriceDto>> getCommodityPrices(String commodity) {
        log.info("Fetching prices for commodity: {}", commodity);
        
        // Try to get from cache first
        String cacheKey = CACHE_KEY_PREFIX + CACHE_KEY_COMMODITY + commodity.toLowerCase();
        List<MandiPriceDto> cachedPrices = getFromCache(cacheKey);
        if (cachedPrices != null && !cachedPrices.isEmpty()) {
            log.info("Found {} prices in cache for commodity: {}", cachedPrices.size(), commodity);
            return Mono.just(cachedPrices);
        }

        // Fetch from AGMARKNET API
        return agmarknetApiClient.getCommodityPrices(commodity)
                .flatMap(priceDtos -> {
                    if (priceDtos.isEmpty()) {
                        log.warn("No prices returned from AGMARKNET for commodity: {}", commodity);
                        return Mono.empty();
                    }
                    
                    // Save to database
                    savePrices(priceDtos);
                    
                    // Cache the results
                    cachePrices(cacheKey, priceDtos);
                    
                    return Mono.just(priceDtos);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // If API fails, try to get from database
                    log.info("AGMARKNET API returned empty, checking database for commodity: {}", commodity);
                    List<MandiPriceDto> dbPrices = getPricesFromDatabase(commodity);
                    if (!dbPrices.isEmpty()) {
                        log.info("Found {} prices in database for commodity: {}", dbPrices.size(), commodity);
                        // Mark as cached
                        dbPrices.forEach(p -> p.setIsCached(true));
                        return Mono.just(dbPrices);
                    }
                    return Mono.empty();
                }));
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
        
        return agmarknetApiClient.getCommodityPricesByState(commodity, state)
                .flatMap(priceDtos -> {
                    if (priceDtos.isEmpty()) {
                        return Mono.empty();
                    }
                    savePrices(priceDtos);
                    return Mono.just(priceDtos);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    List<MandiPriceDto> dbPrices = getPricesFromDatabaseByState(commodity, state);
                    if (!dbPrices.isEmpty()) {
                        dbPrices.forEach(p -> p.setIsCached(true));
                        return Mono.just(dbPrices);
                    }
                    return Mono.empty();
                }));
    }

    /**
     * Fetches historical prices for trend analysis.
     * 
     * @param commodity The commodity name
     * @param days Number of days of historical data
     * @return List of MandiPriceDto with historical prices
     */
    public Mono<List<MandiPriceDto>> getHistoricalPrices(String commodity, int days) {
        log.info("Fetching {} days of historical prices for commodity: {}", days, commodity);
        
        String cacheKey = CACHE_KEY_PREFIX + CACHE_KEY_HISTORY + commodity.toLowerCase() + ":" + days;
        List<MandiPriceDto> cachedPrices = getFromCache(cacheKey);
        if (cachedPrices != null && !cachedPrices.isEmpty()) {
            log.info("Found {} historical prices in cache for commodity: {}", cachedPrices.size(), commodity);
            return Mono.just(cachedPrices);
        }

        return agmarknetApiClient.getHistoricalPrices(commodity, days)
                .flatMap(priceDtos -> {
                    if (priceDtos.isEmpty()) {
                        return Mono.empty();
                    }
                    cachePrices(cacheKey, priceDtos);
                    return Mono.just(priceDtos);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    List<MandiPriceDto> dbPrices = getHistoricalPricesFromDatabase(commodity, days);
                    if (!dbPrices.isEmpty()) {
                        dbPrices.forEach(p -> p.setIsCached(true));
                        return Mono.just(dbPrices);
                    }
                    return Mono.empty();
                }));
    }

    /**
     * Gets the latest prices for a commodity from the database.
     * 
     * @param commodity The commodity name
     * @return List of MandiPriceDto with latest prices
     */
    public List<MandiPriceDto> getLatestPricesFromDatabase(String commodity) {
        List<MandiPrices> prices = mandiPricesRepository.findLatestPricesByCommodity(commodity);
        return prices.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Gets the latest prices for a commodity in a specific state.
     * 
     * @param commodity The commodity name
     * @param state The state name
     * @return List of MandiPriceDto with latest prices
     */
    public List<MandiPriceDto> getLatestPricesFromDatabaseByState(String commodity, String state) {
        List<MandiPrices> prices = mandiPricesRepository.findLatestPricesByCommodityAndState(commodity, state);
        return prices.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Gets historical prices from the database.
     * 
     * @param commodity The commodity name
     * @param days Number of days of historical data
     * @return List of MandiPriceDto with historical prices
     */
    public List<MandiPriceDto> getHistoricalPricesFromDatabase(String commodity, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        List<MandiPrices> prices = mandiPricesRepository.findHistoricalPrices(commodity, startDate, endDate);
        return prices.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Gets all distinct commodities from the database.
     * 
     * @return List of commodity names
     */
    public List<String> getCommodities() {
        return mandiPricesRepository.findDistinctCommodities();
    }

    /**
     * Gets all distinct states from the database.
     * 
     * @return List of state names
     */
    public List<String> getStates() {
        return mandiPricesRepository.findDistinctStates();
    }

    /**
     * Gets all distinct districts for a state.
     * 
     * @param state The state name
     * @return List of district names
     */
    public List<String> getDistricts(String state) {
        return mandiPricesRepository.findDistinctDistrictsByState(state);
    }

    /**
     * Saves price data to the database.
     * 
     * @param priceDtos List of price DTOs to save
     */
    public void savePrices(List<MandiPriceDto> priceDtos) {
        List<MandiPrices> entities = priceDtos.stream()
                .map(this::mapToEntity)
                .collect(Collectors.toList());
        mandiPricesRepository.saveAll(entities);
        log.info("Saved {} price records to database", entities.size());
    }

    /**
     * Gets prices from the database for a commodity.
     * 
     * @param commodity The commodity name
     * @return List of MandiPriceDto
     */
    private List<MandiPriceDto> getPricesFromDatabase(String commodity) {
        List<MandiPrices> prices = mandiPricesRepository.findLatestPricesByCommodity(commodity);
        return prices.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Gets prices from the database for a commodity and state.
     * 
     * @param commodity The commodity name
     * @param state The state name
     * @return List of MandiPriceDto
     */
    private List<MandiPriceDto> getPricesFromDatabaseByState(String commodity, String state) {
        List<MandiPrices> prices = mandiPricesRepository.findLatestPricesByCommodityAndState(commodity, state);
        return prices.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Caches price data in Redis.
     * 
     * @param cacheKey The cache key
     * @param priceDtos List of price DTOs to cache
     */
    private void cachePrices(String cacheKey, List<MandiPriceDto> priceDtos) {
        try {
            redisTemplate.opsForValue().set(cacheKey, priceDtos, cacheTtlHours, TimeUnit.HOURS);
            log.info("Cached {} prices with key: {}", priceDtos.size(), cacheKey);
        } catch (Exception e) {
            log.error("Failed to cache prices: {}", e.getMessage());
        }
    }

    /**
     * Gets price data from Redis cache.
     * 
     * @param cacheKey The cache key
     * @return List of MandiPriceDto or null if not found
     */
    @SuppressWarnings("unchecked")
    private List<MandiPriceDto> getFromCache(String cacheKey) {
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof List) {
                return (List<MandiPriceDto>) cached;
            }
        } catch (Exception e) {
            log.error("Failed to get from cache: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Maps entity to DTO.
     */
    private MandiPriceDto mapToDto(MandiPrices entity) {
        return MandiPriceDto.builder()
                .id(entity.getId())
                .commodityName(entity.getCommodityName())
                .variety(entity.getVariety())
                .mandiName(entity.getMandiName())
                .mandiCode(entity.getMandiCode())
                .state(entity.getState())
                .district(entity.getDistrict())
                .priceDate(entity.getPriceDate())
                .modalPrice(entity.getModalPrice())
                .minPrice(entity.getMinPrice())
                .maxPrice(entity.getMaxPrice())
                .arrivalQuantityQuintals(entity.getArrivalQuantityQuintals())
                .unit(entity.getUnit())
                .source(entity.getSource())
                .fetchedAt(entity.getCreatedAt())
                .isCached(false)
                .build();
    }

    /**
     * Maps DTO to entity.
     */
    private MandiPrices mapToEntity(MandiPriceDto dto) {
        return MandiPrices.builder()
                .commodityName(dto.getCommodityName())
                .variety(dto.getVariety())
                .mandiName(dto.getMandiName())
                .mandiCode(dto.getMandiCode())
                .state(dto.getState())
                .district(dto.getDistrict())
                .priceDate(dto.getPriceDate() != null ? dto.getPriceDate() : LocalDate.now())
                .modalPrice(dto.getModalPrice())
                .minPrice(dto.getMinPrice())
                .maxPrice(dto.getMaxPrice())
                .arrivalQuantityQuintals(dto.getArrivalQuantityQuintals())
                .unit(dto.getUnit() != null ? dto.getUnit() : "Quintal")
                .source(dto.getSource() != null ? dto.getSource() : "AGMARKNET")
                .build();
    }
}