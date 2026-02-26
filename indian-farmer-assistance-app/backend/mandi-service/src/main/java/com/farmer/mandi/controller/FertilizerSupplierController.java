package com.farmer.mandi.controller;

import com.farmer.mandi.dto.FertilizerSupplierDto;
import com.farmer.mandi.service.FertilizerSupplierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Controller for fertilizer supplier operations.
 * 
 * Provides endpoints for searching and discovering fertilizer suppliers
 * by location, type, and other criteria.
 */
@RestController
@RequestMapping("/api/v1/fertilizer/suppliers")
@RequiredArgsConstructor
@Slf4j
public class FertilizerSupplierController {

    private final FertilizerSupplierService fertilizerSupplierService;

    /**
     * Get fertilizer suppliers for a state and district.
     * 
     * @param state State name
     * @param district District name
     * @return List of fertilizer suppliers
     */
    @GetMapping("/location")
    public Mono<ResponseEntity<List<FertilizerSupplierDto>>> getSuppliersByLocation(
            @RequestParam String state,
            @RequestParam String district) {
        log.info("Getting fertilizer suppliers for state: {}, district: {}", state, district);
        
        return fertilizerSupplierService.getSuppliersByLocation(state, district)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.internalServerError().build());
    }

    /**
     * Get fertilizer suppliers for a state.
     * 
     * @param state State name
     * @return List of fertilizer suppliers
     */
    @GetMapping("/state/{state}")
    public Mono<ResponseEntity<List<FertilizerSupplierDto>>> getSuppliersByState(
            @PathVariable String state) {
        log.info("Getting fertilizer suppliers for state: {}", state);
        
        return fertilizerSupplierService.getSuppliersByState(state)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.internalServerError().build());
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
    @GetMapping
    public Mono<ResponseEntity<List<FertilizerSupplierDto>>> getSuppliers(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String district,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit) {
        log.info("Getting fertilizer suppliers: state={}, district={}, offset={}, limit={}", 
                 state, district, offset, limit);
        
        return fertilizerSupplierService.getSuppliers(state, district, offset, limit)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.internalServerError().build());
    }

    /**
     * Get suppliers sorted by number of wholesalers.
     * 
     * @param state State name
     * @param district District name
     * @return List of suppliers sorted by wholesaler count
     */
    @GetMapping("/wholesalers")
    public Mono<ResponseEntity<List<FertilizerSupplierDto>>> getSuppliersByWholesalers(
            @RequestParam String state,
            @RequestParam String district) {
        log.info("Getting fertilizer suppliers by wholesalers: state={}, district={}", state, district);
        
        return fertilizerSupplierService.getSuppliersByWholesalers(state, district)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.internalServerError().build());
    }

    /**
     * Get suppliers sorted by number of retailers.
     * 
     * @param state State name
     * @param district District name
     * @return List of suppliers sorted by retailer count
     */
    @GetMapping("/retailers")
    public Mono<ResponseEntity<List<FertilizerSupplierDto>>> getSuppliersByRetailers(
            @RequestParam String state,
            @RequestParam String district) {
        log.info("Getting fertilizer suppliers by retailers: state={}, district={}", state, district);
        
        return fertilizerSupplierService.getSuppliersByRetailers(state, district)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.internalServerError().build());
    }

    /**
     * Get suppliers for a specific fertilizer type.
     * 
     * @param state State name
     * @param district District name
     * @param fertilizerType Fertilizer type (e.g., Urea, DAP, MOP)
     * @return List of suppliers for the fertilizer type
     */
    @GetMapping("/type/{fertilizerType}")
    public Mono<ResponseEntity<List<FertilizerSupplierDto>>> getSuppliersByFertilizerType(
            @RequestParam String state,
            @RequestParam String district,
            @PathVariable String fertilizerType) {
        log.info("Getting fertilizer suppliers by type: state={}, district={}, type={}", 
                 state, district, fertilizerType);
        
        return fertilizerSupplierService.getSuppliersByFertilizerType(state, district, fertilizerType)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.internalServerError().build());
    }
}
