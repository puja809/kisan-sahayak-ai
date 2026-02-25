package com.farmer.mandi.controller;

import com.farmer.mandi.dto.*;
import com.farmer.mandi.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST controller for mandi price service endpoints.
 * 
 * Requirements:
 * - 6.1: GET /api/v1/mandi/prices/{commodity}
 * - 6.4: GET /api/v1/mandi/prices/nearby
 * - 6.5: GET /api/v1/mandi/prices/trends/{commodity}
 * - 6.6: GET /api/v1/mandi/prices/msp/{commodity}
 * - 6.9: GET /api/v1/mandi/locations/nearby
 * - 6.10: POST /api/v1/mandi/alerts/subscribe
 * - 6.10: GET /api/v1/mandi/alerts/{farmerId}
 */
@RestController
@RequestMapping("/api/v1/mandi")
@RequiredArgsConstructor
@Slf4j
public class MandiController {

    private final MandiPriceService mandiPriceService;
    private final MandiLocationService mandiLocationService;
    private final PriceTrendService priceTrendService;
    private final PriceAlertService priceAlertService;

    /**
     * Gets current prices for a commodity.
     * 
     * GET /api/v1/mandi/prices/{commodity}
     * 
     * @param commodity The commodity name
     * @return List of MandiPriceDto with current prices
     */
    @GetMapping("/prices/{commodity}")
    public Mono<ResponseEntity<List<MandiPriceDto>>> getCommodityPrices(
            @PathVariable String commodity) {
        log.info("Getting prices for commodity: {}", commodity);
        
        return mandiPriceService.getCommodityPrices(commodity)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Gets prices for a commodity in a specific state.
     * 
     * GET /api/v1/mandi/prices/{commodity}/state/{state}
     * 
     * @param commodity The commodity name
     * @param state The state name
     * @return List of MandiPriceDto with prices
     */
    @GetMapping("/prices/{commodity}/state/{state}")
    public Mono<ResponseEntity<List<MandiPriceDto>>> getCommodityPricesByState(
            @PathVariable String commodity,
            @PathVariable String state) {
        log.info("Getting prices for commodity: {} in state: {}", commodity, state);
        
        return mandiPriceService.getCommodityPricesByState(commodity, state)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Gets nearby mandi prices sorted by distance.
     * 
     * GET /api/v1/mandi/prices/nearby
     * 
     * @param commodity The commodity name
     * @param latitude Farmer's latitude
     * @param longitude Farmer's longitude
     * @param radiusKm Search radius in kilometers (default 50)
     * @return List of MandiPriceDto sorted by distance
     */
    @GetMapping("/prices/nearby")
    public Mono<ResponseEntity<List<MandiPriceDto>>> getNearbyPrices(
            @RequestParam String commodity,
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude,
            @RequestParam(defaultValue = "50") int radiusKm) {
        log.info("Getting nearby prices for commodity: {} within {} km", commodity, radiusKm);
        
        return mandiPriceService.getCommodityPrices(commodity)
                .map(prices -> {
                    List<MandiPriceDto> sortedPrices = mandiLocationService.sortPricesByDistance(
                            prices, latitude, longitude);
                    return ResponseEntity.ok(sortedPrices);
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Gets price trends for a commodity.
     * 
     * GET /api/v1/mandi/prices/trends/{commodity}
     * 
     * @param commodity The commodity name
     * @param days Number of days of historical data (default 30)
     * @return PriceTrendDto with trend analysis
     */
    @GetMapping("/prices/trends/{commodity}")
    public Mono<ResponseEntity<PriceTrendDto>> getPriceTrends(
            @PathVariable String commodity,
            @RequestParam(defaultValue = "30") int days) {
        log.info("Getting price trends for commodity: {} over {} days", commodity, days);
        
        return priceTrendService.getPriceTrend(commodity, days)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Gets MSP comparison for a commodity.
     * 
     * GET /api/v1/mandi/prices/msp/{commodity}
     * 
     * @param commodity The commodity name
     * @return MspComparisonDto with MSP vs market price comparison
     */
    @GetMapping("/prices/msp/{commodity}")
    public ResponseEntity<PriceTrendDto.MspComparisonDto> getMspComparison(
            @PathVariable String commodity) {
        log.info("Getting MSP comparison for commodity: {}", commodity);
        
        PriceTrendDto.MspComparisonDto comparison = priceTrendService.getMspComparison(commodity);
        return ResponseEntity.ok(comparison);
    }

    /**
     * Gets storage advisory for a commodity.
     * 
     * GET /api/v1/mandi/prices/advisory/{commodity}
     * 
     * @param commodity The commodity name
     * @return StorageAdvisoryDto with hold/sell recommendation
     */
    @GetMapping("/prices/advisory/{commodity}")
    public ResponseEntity<PriceTrendDto.StorageAdvisoryDto> getStorageAdvisory(
            @PathVariable String commodity) {
        log.info("Getting storage advisory for commodity: {}", commodity);
        
        PriceTrendDto.StorageAdvisoryDto advisory = priceTrendService.getStorageAdvisory(commodity);
        return ResponseEntity.ok(advisory);
    }

    /**
     * Gets nearby mandi locations.
     * 
     * GET /api/v1/mandi/locations/nearby
     * 
     * @param latitude Farmer's latitude
     * @param longitude Farmer's longitude
     * @param radiusKm Search radius in kilometers (default 50)
     * @return List of MandiLocationDto sorted by distance
     */
    @GetMapping("/locations/nearby")
    public ResponseEntity<List<MandiLocationDto>> getNearbyLocations(
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude,
            @RequestParam(defaultValue = "50") int radiusKm) {
        log.info("Getting nearby mandis within {} km", radiusKm);
        
        List<MandiLocationDto> locations = mandiLocationService.getNearbyMandis(
                latitude, longitude, radiusKm);
        return ResponseEntity.ok(locations);
    }

    /**
     * Gets all active mandi locations.
     * 
     * GET /api/v1/mandi/locations
     * 
     * @return List of MandiLocationDto
     */
    @GetMapping("/locations")
    public ResponseEntity<List<MandiLocationDto>> getAllLocations() {
        log.info("Getting all active mandi locations");
        
        List<MandiLocationDto> locations = mandiLocationService.getAllActiveLocations();
        return ResponseEntity.ok(locations);
    }

    /**
     * Gets locations by state.
     * 
     * GET /api/v1/mandi/locations/state/{state}
     * 
     * @param state The state name
     * @return List of MandiLocationDto
     */
    @GetMapping("/locations/state/{state}")
    public ResponseEntity<List<MandiLocationDto>> getLocationsByState(
            @PathVariable String state) {
        log.info("Getting locations for state: {}", state);
        
        List<MandiLocationDto> locations = mandiLocationService.getLocationsByState(state);
        return ResponseEntity.ok(locations);
    }

    /**
     * Subscribes to price alerts.
     * 
     * POST /api/v1/mandi/alerts/subscribe
     * 
     * @param request The alert subscription request
     * @return The created alert DTO
     */
    @PostMapping("/alerts/subscribe")
    public ResponseEntity<PriceAlertDto> subscribeToAlerts(
            @Valid @RequestBody PriceAlertRequest request) {
        log.info("Subscribing to price alerts for farmer: {}, commodity: {}", 
                request.getFarmerId(), request.getCommodity());
        
        PriceAlertDto alert = priceAlertService.createAlert(request);
        return ResponseEntity.ok(alert);
    }

    /**
     * Gets all alerts for a farmer.
     * 
     * GET /api/v1/mandi/alerts/{farmerId}
     * 
     * @param farmerId The farmer ID
     * @return List of PriceAlertDto
     */
    @GetMapping("/alerts/{farmerId}")
    public ResponseEntity<List<PriceAlertDto>> getAlertsForFarmer(
            @PathVariable String farmerId) {
        log.info("Getting alerts for farmer: {}", farmerId);
        
        List<PriceAlertDto> alerts = priceAlertService.getAlertsForFarmer(farmerId);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Unsubscribes from a price alert.
     * 
     * DELETE /api/v1/mandi/alerts/{alertId}
     * 
     * @param alertId The alert ID
     * @return 204 No Content if successful, 404 Not Found if not found
     */
    @DeleteMapping("/alerts/{alertId}")
    public ResponseEntity<Void> unsubscribeFromAlert(
            @PathVariable Long alertId) {
        log.info("Unsubscribing from alert: {}", alertId);
        
        boolean deactivated = priceAlertService.deactivateAlert(alertId);
        if (deactivated) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Gets list of available commodities.
     * 
     * GET /api/v1/mandi/commodities
     * 
     * @return List of commodity names
     */
    @GetMapping("/commodities")
    public ResponseEntity<List<String>> getCommodities() {
        log.info("Getting list of commodities");
        
        List<String> commodities = mandiPriceService.getCommodities();
        return ResponseEntity.ok(commodities);
    }

    /**
     * Gets list of available states.
     * 
     * GET /api/v1/mandi/states
     * 
     * @return List of state names
     */
    @GetMapping("/states")
    public ResponseEntity<List<String>> getStates() {
        log.info("Getting list of states");
        
        List<String> states = mandiPriceService.getStates();
        return ResponseEntity.ok(states);
    }

    /**
     * Gets list of districts for a state.
     * 
     * GET /api/v1/mandi/states/{state}/districts
     * 
     * @param state The state name
     * @return List of district names
     */
    @GetMapping("/states/{state}/districts")
    public ResponseEntity<List<String>> getDistricts(
            @PathVariable String state) {
        log.info("Getting districts for state: {}", state);
        
        List<String> districts = mandiPriceService.getDistricts(state);
        return ResponseEntity.ok(districts);
    }
}