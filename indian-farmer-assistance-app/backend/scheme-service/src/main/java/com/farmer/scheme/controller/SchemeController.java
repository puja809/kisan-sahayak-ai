package com.farmer.scheme.controller;

import com.farmer.scheme.entity.Scheme;
import com.farmer.scheme.entity.Scheme.SchemeType;
import com.farmer.scheme.service.SchemeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for scheme operations.
 * Requirements: 4.1, 4.2, 4.3, 11D.1, 11D.2, 11D.3, 11D.4, 11D.5, 11D.6, 11D.7, 11D.8
 */
@RestController
@RequestMapping("/api/v1/schemes")
@RequiredArgsConstructor
@Slf4j
public class SchemeController {

    private final SchemeService schemeService;

    /**
     * Get all active schemes.
     * GET /api/v1/schemes
     * Requirements: 4.1, 4.2
     */
    @GetMapping
    public ResponseEntity<List<Scheme>> getAllSchemes() {
        log.debug("GET /api/v1/schemes - Fetching all active schemes");
        List<Scheme> schemes = schemeService.getAllActiveSchemes();
        return ResponseEntity.ok(schemes);
    }

    /**
     * Get scheme by ID.
     * GET /api/v1/schemes/{id}
     * Requirements: 4.1, 4.2
     */
    @GetMapping("/{id}")
    public ResponseEntity<Scheme> getSchemeById(@PathVariable Long id) {
        log.debug("GET /api/v1/schemes/{} - Fetching scheme by id", id);
        return schemeService.getSchemeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get scheme by code.
     * GET /api/v1/schemes/code/{code}
     * Requirements: 4.1
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<Scheme> getSchemeByCode(@PathVariable String code) {
        log.debug("GET /api/v1/schemes/code/{} - Fetching scheme by code", code);
        return schemeService.getSchemeByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get schemes by type.
     * GET /api/v1/schemes/type/{type}
     * Requirements: 4.1, 5.1, 5.2
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<Scheme>> getSchemesByType(@PathVariable SchemeType type) {
        log.debug("GET /api/v1/schemes/type/{} - Fetching schemes by type", type);
        List<Scheme> schemes = schemeService.getSchemesByType(type);
        return ResponseEntity.ok(schemes);
    }

    /**
     * Get schemes for a specific state.
     * GET /api/v1/schemes/state/{state}
     * Requirements: 4.3, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9
     */
    @GetMapping("/state/{state}")
    public ResponseEntity<List<Scheme>> getSchemesByState(@PathVariable String state) {
        log.debug("GET /api/v1/schemes/state/{} - Fetching schemes for state", state);
        List<Scheme> schemes = schemeService.getSchemesForState(state);
        return ResponseEntity.ok(schemes);
    }

    /**
     * Get schemes applicable to a specific crop.
     * GET /api/v1/schemes/crop/{cropName}
     * Requirements: 4.1, 11D.4
     */
    @GetMapping("/crop/{cropName}")
    public ResponseEntity<List<Scheme>> getSchemesByCrop(@PathVariable String cropName) {
        log.debug("GET /api/v1/schemes/crop/{} - Fetching schemes for crop", cropName);
        List<Scheme> schemes = schemeService.getSchemesByCrop(cropName);
        return ResponseEntity.ok(schemes);
    }

    /**
     * Get schemes with open application windows.
     * GET /api/v1/schemes/open
     * Requirements: 4.2, 11D.8
     */
    @GetMapping("/open")
    public ResponseEntity<List<Scheme>> getSchemesWithOpenApplications() {
        log.debug("GET /api/v1/schemes/open - Fetching schemes with open applications");
        List<Scheme> schemes = schemeService.getSchemesWithOpenApplications();
        return ResponseEntity.ok(schemes);
    }

    /**
     * Get schemes with approaching deadlines.
     * GET /api/v1/schemes/deadlines?daysAhead=7
     * Requirements: 4.8, 11D.9
     */
    @GetMapping("/deadlines")
    public ResponseEntity<List<Scheme>> getSchemesWithApproachingDeadlines(
            @RequestParam(defaultValue = "7") int daysAhead) {
        log.debug("GET /api/v1/schemes/deadlines?daysAhead={} - Fetching schemes with approaching deadlines", daysAhead);
        List<Scheme> schemes = schemeService.getSchemesWithApproachingDeadlines(daysAhead);
        return ResponseEntity.ok(schemes);
    }

    /**
     * Get all central schemes.
     * GET /api/v1/schemes/central
     * Requirements: 4.1, 5.1
     */
    @GetMapping("/central")
    public ResponseEntity<List<Scheme>> getAllCentralSchemes() {
        log.debug("GET /api/v1/schemes/central - Fetching all central schemes");
        List<Scheme> schemes = schemeService.getAllCentralSchemes();
        return ResponseEntity.ok(schemes);
    }

    /**
     * Create a new scheme (admin only).
     * POST /api/v1/schemes
     * Requirements: 4.1, 21.9
     */
    @PostMapping
    public ResponseEntity<Scheme> createScheme(@RequestBody Scheme scheme) {
        log.info("POST /api/v1/schemes - Creating new scheme: {}", scheme.getSchemeName());
        Scheme createdScheme = schemeService.createScheme(scheme);
        return ResponseEntity.ok(createdScheme);
    }

    /**
     * Update an existing scheme (admin only).
     * PUT /api/v1/schemes/{id}
     * Requirements: 4.1, 21.10
     */
    @PutMapping("/{id}")
    public ResponseEntity<Scheme> updateScheme(@PathVariable Long id, @RequestBody Scheme schemeDetails) {
        log.info("PUT /api/v1/schemes/{} - Updating scheme", id);
        try {
            Scheme updatedScheme = schemeService.updateScheme(id, schemeDetails);
            return ResponseEntity.ok(updatedScheme);
        } catch (RuntimeException e) {
            log.error("Error updating scheme: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Deactivate a scheme (admin only).
     * DELETE /api/v1/schemes/{id}
     * Requirements: 4.1
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateScheme(@PathVariable Long id) {
        log.info("DELETE /api/v1/schemes/{} - Deactivating scheme", id);
        schemeService.deactivateScheme(id);
        return ResponseEntity.noContent().build();
    }
}