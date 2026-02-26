package com.farmer.scheme.controller;

import com.farmer.scheme.dto.SchemeStatistics;
import com.farmer.scheme.entity.Scheme;
import com.farmer.scheme.entity.Scheme.SchemeType;
import com.farmer.scheme.service.SchemeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for admin-only scheme management operations.
 * Requirements: 4.1, 4.2, 4.3, 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9
 */
@RestController
@RequestMapping("/api/v1/admin/schemes")
@RequiredArgsConstructor
@Slf4j
// @PreAuthorize("hasRole('ADMIN')")
public class AdminSchemeController {

    private final SchemeService schemeService;

    /**
     * Create a new scheme.
     * POST /api/v1/admin/schemes
     * Requirements: 4.1, 21.9
     */
    @PostMapping
    public ResponseEntity<Scheme> createScheme(@RequestBody Scheme scheme) {
        log.info("POST /api/v1/admin/schemes - Creating new scheme: {}", scheme.getSchemeName());
        Scheme createdScheme = schemeService.createScheme(scheme);
        return ResponseEntity.ok(createdScheme);
    }

    /**
     * Update an existing scheme.
     * PUT /api/v1/admin/schemes/{id}
     * Requirements: 4.1, 21.10
     */
    @PutMapping("/{id}")
    public ResponseEntity<Scheme> updateScheme(@PathVariable Long id, @RequestBody Scheme schemeDetails) {
        log.info("PUT /api/v1/admin/schemes/{} - Updating scheme", id);
        try {
            Scheme updatedScheme = schemeService.updateScheme(id, schemeDetails);
            return ResponseEntity.ok(updatedScheme);
        } catch (RuntimeException e) {
            log.error("Error updating scheme: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Deactivate a scheme (soft delete).
     * DELETE /api/v1/admin/schemes/{id}
     * Requirements: 4.1
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateScheme(@PathVariable Long id) {
        log.info("DELETE /api/v1/admin/schemes/{} - Deactivating scheme", id);
        schemeService.deactivateScheme(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Activate a deactivated scheme.
     * PUT /api/v1/admin/schemes/{id}/activate
     * Requirements: 4.1
     */
    @PutMapping("/{id}/activate")
    public ResponseEntity<Scheme> activateScheme(@PathVariable Long id) {
        log.info("PUT /api/v1/admin/schemes/{}/activate - Activating scheme", id);
        try {
            Scheme activatedScheme = schemeService.activateScheme(id);
            return ResponseEntity.ok(activatedScheme);
        } catch (RuntimeException e) {
            log.error("Error activating scheme: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all schemes including inactive ones.
     * GET /api/v1/admin/schemes/all
     * Requirements: 4.1
     */
    @GetMapping("/all")
    public ResponseEntity<List<Scheme>> getAllSchemes() {
        log.debug("GET /api/v1/admin/schemes/all - Fetching all schemes including inactive");
        List<Scheme> schemes = schemeService.getAllSchemes();
        return ResponseEntity.ok(schemes);
    }

    /**
     * Get scheme statistics.
     * GET /api/v1/admin/schemes/statistics
     * Requirements: 4.1
     */
    @GetMapping("/statistics")
    public ResponseEntity<SchemeStatistics> getSchemeStatistics() {
        log.debug("GET /api/v1/admin/schemes/statistics - Fetching scheme statistics");
        SchemeStatistics stats = schemeService.getSchemeStatistics();
        return ResponseEntity.ok(stats);
    }
}