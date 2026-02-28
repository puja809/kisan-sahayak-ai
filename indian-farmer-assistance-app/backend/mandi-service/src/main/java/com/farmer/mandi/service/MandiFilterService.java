package com.farmer.mandi.service;

import com.farmer.mandi.client.DataGovInApiClient;
import com.farmer.mandi.dto.MandiPriceDto;
import com.farmer.mandi.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for filtering Mandi market data
 */
@Service
@Slf4j
public class MandiFilterService {
    
    @Autowired
    private StateRepository stateRepository;
    
    @Autowired
    private DistrictRepository districtRepository;
    
    @Autowired
    private MandiLocationRepository mandiLocationRepository;
    
    @Autowired
    private CommodityRepository commodityRepository;
    
    @Autowired
    private VarietyRepository varietyRepository;
    
    @Autowired
    private GradeRepository gradeRepository;
    
    @Autowired
    private DataGovInApiClient dataGovInApiClient;
    
    /**
     * Get all states
     */
    public List<String> getAllStates() {
        return stateRepository.findAll().stream()
            .map(s -> s.getStateName())
            .sorted()
            .toList();
    }
    
    /**
     * Get districts by state
     */
    public List<String> getDistrictsByState(String state) {
        if (state == null || state.isEmpty()) {
            return List.of();
        }
        return districtRepository.findByState_StateName(state).stream()
            .map(d -> d.getDistrictName())
            .sorted()
            .toList();
    }
    
    /**
     * Get markets by state and district
     */
    public List<String> getMarketsByStateAndDistrict(String state, String district) {
        if (state == null || state.isEmpty() || district == null || district.isEmpty()) {
            return List.of();
        }
        return mandiLocationRepository.findByState_StateNameAndDistrict_DistrictName(state, district).stream()
            .map(m -> m.getMandiName())
            .sorted()
            .toList();
    }
    
    /**
     * Get commodities by market (returns all commodities)
     */
    public List<String> getCommoditiesByMarket(String market) {
        if (market == null || market.isEmpty()) {
            return List.of();
        }
        return getAllCommodities();
    }
    
    /**
     * Get varieties by commodity
     */
    public List<String> getVarietiesByCommodity(String commodity) {
        if (commodity == null || commodity.isEmpty()) {
            return List.of();
        }
        return commodityRepository.findByCommodityName(commodity)
            .map(c -> varietyRepository.findByCommodityAndIsActiveTrueOrderByVarietyName(c).stream()
                .map(v -> v.getVarietyName())
                .sorted()
                .toList())
            .orElse(List.of());
    }
    
    /**
     * Get grades by commodity and variety
     */
    public List<String> getGradesByCommodityAndVariety(String commodity, String variety) {
        if (commodity == null || commodity.isEmpty() || variety == null || variety.isEmpty()) {
            return List.of();
        }
        return commodityRepository.findByCommodityName(commodity)
            .flatMap(c -> varietyRepository.findByCommodityAndVarietyName(c, variety)
                .map(v -> gradeRepository.findByVarietyAndIsActiveTrue(v).stream()
                    .map(g -> g.getGradeName())
                    .sorted()
                    .toList()))
            .orElse(List.of());
    }
    
    /**
     * Get all commodities
     */
    public List<String> getAllCommodities() {
        return commodityRepository.findAllDistinctCommodityNames();
    }
    
    /**
     * Get all varieties
     */
    public List<String> getAllVarieties() {
        return varietyRepository.findAllDistinctVarietyNames();
    }
    
    /**
     * Get all grades
     */
    public List<String> getAllGrades() {
        return gradeRepository.findAllDistinctGrades();
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
     * Search market data with filters - fetches commodity and price data from government portal
     */
    public List<MandiPriceDto> searchMarketData(
        String state,
        String district,
        String market,
        String commodity,
        String variety,
        String grade,
        int offset,
        int limit) {
        
        log.info("Searching market data: state={}, district={}, market={}, commodity={}, variety={}, grade={}, offset={}, limit={}",
                 state, district, market, commodity, variety, grade, offset, limit);
        
        try {
            // Fetch mandi prices from government portal with pagination
            List<DataGovInApiClient.MandiPriceRecord> priceRecords = dataGovInApiClient.getMandiPrices(
                state, district, market, commodity, offset, limit)
                .block(); // Block to get synchronous result
            
            if (priceRecords == null || priceRecords.isEmpty()) {
                log.warn("No price records found for the given filters");
                return List.of();
            }
            
            // Convert records to DTOs and apply additional filters
            return priceRecords.stream()
                .map(this::convertToMandiPriceDto)
                .filter(dto -> applyVarietyFilter(dto, variety))
                .filter(dto -> applyGradeFilter(dto, grade))
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("Error searching market data: {}", e.getMessage(), e);
            return List.of();
        }
    }
    
    /**
     * Convert MandiPriceRecord to MandiPriceDto
     */
    private MandiPriceDto convertToMandiPriceDto(DataGovInApiClient.MandiPriceRecord record) {
        return MandiPriceDto.builder()
            .commodityName(record.getCommodity())
            .variety(record.getVariety())
            .mandiName(record.getMarket())
            .state(record.getState())
            .district(record.getDistrict())
            .modalPrice(record.getModalPrice())
            .minPrice(record.getMinPrice())
            .maxPrice(record.getMaxPrice())
            .arrivalQuantityQuintals(record.getArrivalQuantity())
            .unit(record.getUnit())
            .source("DataGovIn")
            .build();
    }
    
    /**
     * Apply variety filter to price DTO
     */
    private boolean applyVarietyFilter(MandiPriceDto dto, String variety) {
        if (variety == null || variety.isEmpty()) {
            return true;
        }
        return dto.getVariety() != null && dto.getVariety().equalsIgnoreCase(variety);
    }
    
    /**
     * Apply grade filter to price DTO
     */
    private boolean applyGradeFilter(MandiPriceDto dto, String grade) {
        if (grade == null || grade.isEmpty()) {
            return true;
        }
        // Grade filtering can be extended based on data availability
        return true;
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
