package com.farmer.mandi.service;

import com.farmer.mandi.entity.MandiMarketData;
import com.farmer.mandi.repository.MandiMarketDataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for filtering Mandi market data
 */
@Service
@Slf4j
public class MandiFilterService {
    
    @Autowired
    private MandiMarketDataRepository mandiMarketDataRepository;
    
    /**
     * Get all states
     */
    public List<String> getAllStates() {
        return mandiMarketDataRepository.findAllStates();
    }
    
    /**
     * Get districts by state
     */
    public List<String> getDistrictsByState(String state) {
        if (state == null || state.isEmpty()) {
            return List.of();
        }
        return mandiMarketDataRepository.findDistrictsByState(state);
    }
    
    /**
     * Get markets by state and district
     */
    public List<String> getMarketsByStateAndDistrict(String state, String district) {
        if (state == null || state.isEmpty() || district == null || district.isEmpty()) {
            return List.of();
        }
        return mandiMarketDataRepository.findMarketsByStateAndDistrict(state, district);
    }
    
    /**
     * Get commodities by market
     */
    public List<String> getCommoditiesByMarket(String market) {
        if (market == null || market.isEmpty()) {
            return List.of();
        }
        return mandiMarketDataRepository.findCommoditiesByMarket(market);
    }
    
    /**
     * Get varieties by commodity
     */
    public List<String> getVarietiesByCommodity(String commodity) {
        if (commodity == null || commodity.isEmpty()) {
            return List.of();
        }
        return mandiMarketDataRepository.findVarietiesByCommodity(commodity);
    }
    
    /**
     * Get grades by commodity and variety
     */
    public List<String> getGradesByCommodityAndVariety(String commodity, String variety) {
        if (commodity == null || commodity.isEmpty() || variety == null || variety.isEmpty()) {
            return List.of();
        }
        return mandiMarketDataRepository.findGradesByCommodityAndVariety(commodity, variety);
    }
    
    /**
     * Get all commodities
     */
    public List<String> getAllCommodities() {
        return mandiMarketDataRepository.findAllCommodities();
    }
    
    /**
     * Get all varieties
     */
    public List<String> getAllVarieties() {
        return mandiMarketDataRepository.findAllVarieties();
    }
    
    /**
     * Get all grades
     */
    public List<String> getAllGrades() {
        return mandiMarketDataRepository.findAllGrades();
    }
    
    /**
     * Search market data with filters
     */
    public List<MandiMarketData> searchMarketData(
        String state,
        String district,
        String market,
        String commodity,
        String variety,
        String grade) {
        
        return mandiMarketDataRepository.findByFilters(state, district, market, commodity, variety, grade);
    }
    
    /**
     * Get filter options for UI
     */
    public FilterOptions getFilterOptions() {
        return FilterOptions.builder()
            .states(getAllStates())
            .commodities(getAllCommodities())
            .varieties(getAllVarieties())
            .grades(getAllGrades())
            .build();
    }
    
    /**
     * Get filter options by state
     */
    public FilterOptions getFilterOptionsByState(String state) {
        return FilterOptions.builder()
            .states(getAllStates())
            .districts(getDistrictsByState(state))
            .commodities(getAllCommodities())
            .varieties(getAllVarieties())
            .grades(getAllGrades())
            .build();
    }
    
    /**
     * Get filter options by state and district
     */
    public FilterOptions getFilterOptionsByStateAndDistrict(String state, String district) {
        return FilterOptions.builder()
            .states(getAllStates())
            .districts(getDistrictsByState(state))
            .markets(getMarketsByStateAndDistrict(state, district))
            .commodities(getAllCommodities())
            .varieties(getAllVarieties())
            .grades(getAllGrades())
            .build();
    }
    
    /**
     * Filter options class
     */
    @lombok.Data
    @lombok.Builder
    public static class FilterOptions {
        private List<String> states;
        private List<String> districts;
        private List<String> markets;
        private List<String> commodities;
        private List<String> varieties;
        private List<String> grades;
    }
}
