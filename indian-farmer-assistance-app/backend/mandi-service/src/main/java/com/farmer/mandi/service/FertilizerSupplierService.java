package com.farmer.mandi.service;

import com.farmer.mandi.client.DataGovInApiClient;
import com.farmer.mandi.dto.FertilizerSupplierDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing fertilizer supplier information.
 * 
 * Fetches supplier data from data.gov.in API and provides
 * location-based filtering and search capabilities.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FertilizerSupplierService {

    private final DataGovInApiClient dataGovInApiClient;

    /**
     * Get fertilizer suppliers for a state and district.
     * 
     * @param state State name
     * @param district District name
     * @return List of fertilizer suppliers
     */
    @Cacheable(value = "fertilizer_suppliers", key = "#state + '_' + #district")
    public Mono<List<FertilizerSupplierDto>> getSuppliersByLocation(String state, String district) {
        log.info("Fetching fertilizer suppliers for state: {}, district: {}", state, district);

        return dataGovInApiClient.getFertilizerSuppliers(state, district, 0, 100)
                .map(records -> records.stream()
                        .map(this::mapToDto)
                        .collect(Collectors.toList()))
                .doOnNext(suppliers -> log.info("Found {} fertilizer suppliers", suppliers.size()))
                .doOnError(error -> log.error("Error fetching fertilizer suppliers: {}", error.getMessage()));
    }

    /**
     * Get fertilizer suppliers for a state.
     * 
     * @param state State name
     * @return List of fertilizer suppliers
     */
    @Cacheable(value = "fertilizer_suppliers_state", key = "#state")
    public Mono<List<FertilizerSupplierDto>> getSuppliersByState(String state) {
        log.info("Fetching fertilizer suppliers for state: {}", state);

        return dataGovInApiClient.getFertilizerSuppliers(state, null, 0, 100)
                .map(records -> records.stream()
                        .map(this::mapToDto)
                        .collect(Collectors.toList()))
                .doOnNext(suppliers -> log.info("Found {} fertilizer suppliers in state", suppliers.size()))
                .doOnError(error -> log.error("Error fetching fertilizer suppliers: {}", error.getMessage()));
    }

    /**
     * Get fertilizer suppliers with pagination.
     * 
     * @param state State name
     * @param district District name
     * @param offset Pagination offset
     * @param limit Results per page
     * @return List of fertilizer suppliers
     */
    public Mono<List<FertilizerSupplierDto>> getSuppliers(String state, String district, int offset, int limit) {
        log.info("Fetching fertilizer suppliers: state={}, district={}, offset={}, limit={}", 
                 state, district, offset, limit);

        return dataGovInApiClient.getFertilizerSuppliers(state, district, offset, limit)
                .map(records -> records.stream()
                        .map(this::mapToDto)
                        .collect(Collectors.toList()))
                .doOnError(error -> log.error("Error fetching fertilizer suppliers: {}", error.getMessage()));
    }

    /**
     * Get suppliers with most wholesalers.
     * 
     * @param state State name
     * @param district District name
     * @return List of suppliers sorted by wholesaler count
     */
    public Mono<List<FertilizerSupplierDto>> getSuppliersByWholesalers(String state, String district) {
        return getSuppliersByLocation(state, district)
                .map(suppliers -> suppliers.stream()
                        .sorted((s1, s2) -> Integer.compare(s2.getNoOfWholesalers(), s1.getNoOfWholesalers()))
                        .collect(Collectors.toList()));
    }

    /**
     * Get suppliers with most retailers.
     * 
     * @param state State name
     * @param district District name
     * @return List of suppliers sorted by retailer count
     */
    public Mono<List<FertilizerSupplierDto>> getSuppliersByRetailers(String state, String district) {
        return getSuppliersByLocation(state, district)
                .map(suppliers -> suppliers.stream()
                        .sorted((s1, s2) -> Integer.compare(s2.getNoOfRetailers(), s1.getNoOfRetailers()))
                        .collect(Collectors.toList()));
    }

    /**
     * Get suppliers for a specific fertilizer type.
     * 
     * @param state State name
     * @param district District name
     * @param fertilizerType Fertilizer type
     * @return List of suppliers for the fertilizer type
     */
    public Mono<List<FertilizerSupplierDto>> getSuppliersByFertilizerType(
            String state, String district, String fertilizerType) {
        return getSuppliersByLocation(state, district)
                .map(suppliers -> suppliers.stream()
                        .filter(s -> s.getFertilizerType() != null && 
                                   s.getFertilizerType().equalsIgnoreCase(fertilizerType))
                        .collect(Collectors.toList()));
    }

    /**
     * Map API record to DTO.
     * 
     * @param record API record
     * @return DTO
     */
    private FertilizerSupplierDto mapToDto(DataGovInApiClient.FertilizerSupplierRecord record) {
        return FertilizerSupplierDto.builder()
                .state(record.getState())
                .district(record.getDistrict())
                .documentId(record.getDocumentId())
                .slNo(record.getSlNo())
                .noOfWholesalers(record.getNoOfWholesalers())
                .noOfRetailers(record.getNoOfRetailers())
                .fertilizerType(record.getFertilizerType())
                .supplierName(record.getSupplierName())
                .contactInfo(record.getContactInfo())
                .build();
    }
}
