package com.farmer.mandi.controller;

import com.farmer.mandi.entity.MandiMarketData;
import com.farmer.mandi.service.MandiFilterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for Mandi market data filtering
 */
@RestController
@RequestMapping("/api/mandi/filter")
@Tag(name = "Mandi Filters", description = "APIs for filtering market data")
@Slf4j
public class MandiFilterController {
    
    @Autowired
    private MandiFilterService mandiFilterService;
    
    /**
     * Get all states
     */
    @GetMapping("/states")
    @Operation(summary = "Get all states", description = "Get list of all states with market data")
    public ResponseEntity<List<String>> getStates() {
        return ResponseEntity.ok(mandiFilterService.getAllStates());
    }
    
    /**
     * Get districts by state
     */
    @GetMapping("/districts")
    @Operation(summary = "Get districts by state", description = "Get list of districts for a specific state")
    public ResponseEntity<List<String>> getDistricts(
        @Parameter(description = "State name") @RequestParam String state) {
        return ResponseEntity.ok(mandiFilterService.getDistrictsByState(state));
    }
    
    /**
     * Get markets by state and district
     */
    @GetMapping("/markets")
    @Operation(summary = "Get markets", description = "Get list of markets for state and district")
    public ResponseEntity<List<String>> getMarkets(
        @Parameter(description = "State name") @RequestParam String state,
        @Parameter(description = "District name") @RequestParam String district) {
        return ResponseEntity.ok(mandiFilterService.getMarketsByStateAndDistrict(state, district));
    }
    
    /**
     * Get commodities by market
     */
    @GetMapping("/commodities")
    @Operation(summary = "Get commodities", description = "Get list of commodities for a market")
    public ResponseEntity<List<String>> getCommodities(
        @Parameter(description = "Market name") @RequestParam(required = false) String market) {
        if (market != null && !market.isEmpty()) {
            return ResponseEntity.ok(mandiFilterService.getCommoditiesByMarket(market));
        }
        return ResponseEntity.ok(mandiFilterService.getAllCommodities());
    }
    
    /**
     * Get varieties by commodity
     */
    @GetMapping("/varieties")
    @Operation(summary = "Get varieties", description = "Get list of varieties for a commodity")
    public ResponseEntity<List<String>> getVarieties(
        @Parameter(description = "Commodity name") @RequestParam(required = false) String commodity) {
        if (commodity != null && !commodity.isEmpty()) {
            return ResponseEntity.ok(mandiFilterService.getVarietiesByCommodity(commodity));
        }
        return ResponseEntity.ok(mandiFilterService.getAllVarieties());
    }
    
    /**
     * Get grades by commodity and variety
     */
    @GetMapping("/grades")
    @Operation(summary = "Get grades", description = "Get list of grades for commodity and variety")
    public ResponseEntity<List<String>> getGrades(
        @Parameter(description = "Commodity name") @RequestParam(required = false) String commodity,
        @Parameter(description = "Variety name") @RequestParam(required = false) String variety) {
        if (commodity != null && !commodity.isEmpty() && variety != null && !variety.isEmpty()) {
            return ResponseEntity.ok(mandiFilterService.getGradesByCommodityAndVariety(commodity, variety));
        }
        return ResponseEntity.ok(mandiFilterService.getAllGrades());
    }
    
    /**
     * Search market data with filters
     */
    @GetMapping("/search")
    @Operation(summary = "Search market data", description = "Search market data with multiple filters")
    public ResponseEntity<List<MandiMarketData>> searchMarketData(
        @Parameter(description = "State name") @RequestParam(required = false) String state,
        @Parameter(description = "District name") @RequestParam(required = false) String district,
        @Parameter(description = "Market name") @RequestParam(required = false) String market,
        @Parameter(description = "Commodity name") @RequestParam(required = false) String commodity,
        @Parameter(description = "Variety name") @RequestParam(required = false) String variety,
        @Parameter(description = "Grade name") @RequestParam(required = false) String grade) {
        
        List<MandiMarketData> results = mandiFilterService.searchMarketData(
            state, district, market, commodity, variety, grade);
        return ResponseEntity.ok(results);
    }
    
    /**
     * Get all filter options
     */
    @GetMapping("/options")
    @Operation(summary = "Get all filter options", description = "Get all available filter options")
    public ResponseEntity<MandiFilterService.FilterOptions> getFilterOptions() {
        return ResponseEntity.ok(mandiFilterService.getFilterOptions());
    }
    
    /**
     * Get filter options by state
     */
    @GetMapping("/options/by-state")
    @Operation(summary = "Get filter options by state", description = "Get filter options for a specific state")
    public ResponseEntity<MandiFilterService.FilterOptions> getFilterOptionsByState(
        @Parameter(description = "State name") @RequestParam String state) {
        return ResponseEntity.ok(mandiFilterService.getFilterOptionsByState(state));
    }
    
    /**
     * Get filter options by state and district
     */
    @GetMapping("/options/by-state-district")
    @Operation(summary = "Get filter options by state and district", description = "Get filter options for state and district")
    public ResponseEntity<MandiFilterService.FilterOptions> getFilterOptionsByStateAndDistrict(
        @Parameter(description = "State name") @RequestParam String state,
        @Parameter(description = "District name") @RequestParam String district) {
        return ResponseEntity.ok(mandiFilterService.getFilterOptionsByStateAndDistrict(state, district));
    }
}
