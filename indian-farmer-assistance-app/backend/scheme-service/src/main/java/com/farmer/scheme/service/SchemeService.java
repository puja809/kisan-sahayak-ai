package com.farmer.scheme.service;

import com.farmer.scheme.dto.SchemeStatistics;
import com.farmer.scheme.entity.Scheme;
import com.farmer.scheme.entity.Scheme.SchemeType;
import com.farmer.scheme.repository.SchemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing government schemes.
 * Requirements: 4.1, 4.2, 4.3, 11D.1, 11D.2, 11D.3, 11D.4, 11D.5, 11D.6, 11D.7, 11D.8
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SchemeService {

    private final SchemeRepository schemeRepository;

    /**
     * Get all active schemes.
     * Requirements: 4.1, 4.2
     */
    @Transactional(readOnly = true)
    public List<Scheme> getAllActiveSchemes() {
        log.debug("Fetching all active schemes");
        return schemeRepository.findByIsActiveTrue();
    }

    /**
     * Get scheme by ID.
     * Requirements: 4.1, 4.2
     */
    @Transactional(readOnly = true)
    public Optional<Scheme> getSchemeById(Long id) {
        log.debug("Fetching scheme with id: {}", id);
        return schemeRepository.findById(id);
    }

    /**
     * Get scheme by scheme code.
     * Requirements: 4.1
     */
    @Transactional(readOnly = true)
    public Optional<Scheme> getSchemeByCode(String schemeCode) {
        log.debug("Fetching scheme with code: {}", schemeCode);
        return schemeRepository.findBySchemeCode(schemeCode);
    }

    /**
     * Get schemes by type.
     * Requirements: 4.1, 5.1, 5.2
     */
    @Transactional(readOnly = true)
    public List<Scheme> getSchemesByType(SchemeType schemeType) {
        log.debug("Fetching schemes by type: {}", schemeType);
        return schemeRepository.findBySchemeTypeAndIsActiveTrue(schemeType);
    }

    /**
     * Get schemes for a specific state (central + state-specific).
     * Requirements: 4.3, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9
     */
    @Transactional(readOnly = true)
    public List<Scheme> getSchemesForState(String state) {
        log.debug("Fetching schemes for state: {}", state);
        return schemeRepository.findActiveSchemesForState(state);
    }

    /**
     * Get schemes applicable to a specific crop.
     * Requirements: 4.1, 11D.4
     */
    @Transactional(readOnly = true)
    public List<Scheme> getSchemesByCrop(String cropName) {
        log.debug("Fetching schemes for crop: {}", cropName);
        return schemeRepository.findByApplicableCrop(cropName);
    }

    /**
     * Get schemes with open application windows.
     * Requirements: 4.2, 11D.8
     */
    @Transactional(readOnly = true)
    public List<Scheme> getSchemesWithOpenApplications() {
        log.debug("Fetching schemes with open applications");
        return schemeRepository.findActiveSchemesWithOpenApplications(LocalDate.now());
    }

    /**
     * Get schemes with approaching deadlines.
     * Requirements: 4.8, 11D.9
     */
    @Transactional(readOnly = true)
    public List<Scheme> getSchemesWithApproachingDeadlines(int daysAhead) {
        log.debug("Fetching schemes with deadlines within {} days", daysAhead);
        LocalDate currentDate = LocalDate.now();
        LocalDate deadlineDate = currentDate.plusDays(daysAhead);
        return schemeRepository.findSchemesWithApproachingDeadlines(currentDate, deadlineDate);
    }

    /**
     * Create a new scheme.
     * Requirements: 4.1, 21.9
     */
    public Scheme createScheme(Scheme scheme) {
        log.info("Creating new scheme: {}", scheme.getSchemeName());
        return schemeRepository.save(scheme);
    }

    /**
     * Update an existing scheme.
     * Requirements: 4.1, 21.10
     */
    public Scheme updateScheme(Long id, Scheme schemeDetails) {
        log.info("Updating scheme with id: {}", id);
        return schemeRepository.findById(id)
                .map(existingScheme -> {
                    existingScheme.setSchemeName(schemeDetails.getSchemeName());
                    existingScheme.setSchemeType(schemeDetails.getSchemeType());
                    existingScheme.setState(schemeDetails.getState());
                    existingScheme.setDescription(schemeDetails.getDescription());
                    existingScheme.setEligibilityCriteria(schemeDetails.getEligibilityCriteria());
                    existingScheme.setBenefitAmount(schemeDetails.getBenefitAmount());
                    existingScheme.setBenefitDescription(schemeDetails.getBenefitDescription());
                    existingScheme.setApplicationStartDate(schemeDetails.getApplicationStartDate());
                    existingScheme.setApplicationEndDate(schemeDetails.getApplicationEndDate());
                    existingScheme.setApplicationUrl(schemeDetails.getApplicationUrl());
                    existingScheme.setContactInfo(schemeDetails.getContactInfo());
                    existingScheme.setIsActive(schemeDetails.getIsActive());
                    existingScheme.setApplicableCrops(schemeDetails.getApplicableCrops());
                    existingScheme.setSubsidyPercentage(schemeDetails.getSubsidyPercentage());
                    existingScheme.setMaxBenefitAmount(schemeDetails.getMaxBenefitAmount());
                    existingScheme.setLandholdingRequirement(schemeDetails.getLandholdingRequirement());
                    existingScheme.setTargetBeneficiaries(schemeDetails.getTargetBeneficiaries());
                    return schemeRepository.save(existingScheme);
                })
                .orElseThrow(() -> new RuntimeException("Scheme not found with id: " + id));
    }

    /**
     * Deactivate a scheme.
     * Requirements: 4.1
     */
    public void deactivateScheme(Long id) {
        log.info("Deactivating scheme with id: {}", id);
        schemeRepository.findById(id)
                .ifPresent(scheme -> {
                    scheme.setIsActive(false);
                    schemeRepository.save(scheme);
                });
    }

    /**
     * Get all central schemes.
     * Requirements: 4.1, 5.1
     */
    @Transactional(readOnly = true)
    public List<Scheme> getAllCentralSchemes() {
        log.debug("Fetching all central schemes");
        return schemeRepository.findAllCentralSchemes();
    }

    /**
     * Get state-specific schemes.
     * Requirements: 4.3, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9
     */
    @Transactional(readOnly = true)
    public List<Scheme> getStateSchemes(String state) {
        log.debug("Fetching state schemes for: {}", state);
        return schemeRepository.findStateSchemes(state);
    }

    /**
     * Get all schemes including inactive ones (admin only).
     * Requirements: 4.1
     */
    @Transactional(readOnly = true)
    public List<Scheme> getAllSchemes() {
        log.debug("Fetching all schemes including inactive");
        return schemeRepository.findAll();
    }

    /**
     * Activate a deactivated scheme.
     * Requirements: 4.1
     */
    public Scheme activateScheme(Long id) {
        log.info("Activating scheme with id: {}", id);
        return schemeRepository.findById(id)
                .map(scheme -> {
                    scheme.setIsActive(true);
                    return schemeRepository.save(scheme);
                })
                .orElseThrow(() -> new RuntimeException("Scheme not found with id: " + id));
    }

    /**
     * Get scheme statistics.
     * Requirements: 4.1
     */
    @Transactional(readOnly = true)
    public SchemeStatistics getSchemeStatistics() {
        log.debug("Calculating scheme statistics");
        long totalSchemes = schemeRepository.count();
        long activeSchemes = schemeRepository.countByIsActiveTrue();
        long centralSchemes = schemeRepository.countBySchemeTypeAndIsActiveTrue(SchemeType.CENTRAL);
        long stateSchemes = schemeRepository.countBySchemeTypeAndIsActiveTrue(SchemeType.STATE);
        long cropSpecificSchemes = schemeRepository.countBySchemeTypeAndIsActiveTrue(SchemeType.CROP_SPECIFIC);
        long insuranceSchemes = schemeRepository.countBySchemeTypeAndIsActiveTrue(SchemeType.INSURANCE);
        long subsidySchemes = schemeRepository.countBySchemeTypeAndIsActiveTrue(SchemeType.SUBSIDY);
        long welfareSchemes = schemeRepository.countBySchemeTypeAndIsActiveTrue(SchemeType.WELFARE);

        return new SchemeStatistics(
                totalSchemes, activeSchemes, centralSchemes, stateSchemes,
                cropSpecificSchemes, insuranceSchemes, subsidySchemes, welfareSchemes);
    }
}