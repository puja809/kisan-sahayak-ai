package com.farmer.crop.service;

import com.farmer.crop.client.KaegroCropSoilApiClient;
import com.farmer.crop.dto.SoilDataDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for fetching and processing soil data from Kaegro API.
 * Provides soil information for crop recommendations and fertilizer suggestions.
 */
@Slf4j
@Service
public class SoilDataService {

    private final KaegroCropSoilApiClient kaegroCropSoilApiClient;

    public SoilDataService(KaegroCropSoilApiClient kaegroCropSoilApiClient) {
        this.kaegroCropSoilApiClient = kaegroCropSoilApiClient;
    }

    /**
     * Fetch soil data for given coordinates
     * 
     * @param latitude GPS latitude
     * @param longitude GPS longitude
     * @return Soil data DTO or null if fetch fails
     */
    public SoilDataDto getSoilData(Double latitude, Double longitude) {
        try {
            log.info("Fetching soil data for coordinates: {}, {}", latitude, longitude);
            
            KaegroCropSoilApiClient.SoilDataResponse response = 
                    kaegroCropSoilApiClient.getSoilData(latitude, longitude);
            
            if (response == null) {
                log.warn("No soil data received from Kaegro API");
                return null;
            }
            
            return convertToDto(response);
        } catch (Exception e) {
            log.error("Error fetching soil data: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Convert API response to DTO
     */
    private SoilDataDto convertToDto(KaegroCropSoilApiClient.SoilDataResponse response) {
        return SoilDataDto.builder()
                .textureClass(response.getSoilType() != null ? response.getSoilType().getTextureClass() : null)
                .faoClassification(response.getSoilType() != null ? response.getSoilType().getFaoClassification() : null)
                .sandPct(response.getPhysical() != null ? response.getPhysical().getSandPct() : null)
                .siltPct(response.getPhysical() != null ? response.getPhysical().getSiltPct() : null)
                .clayPct(response.getPhysical() != null ? response.getPhysical().getClayPct() : null)
                .bulkDensityGCm3(response.getPhysical() != null ? response.getPhysical().getBulkDensityGCm3() : null)
                .phH2o(response.getChemical() != null ? response.getChemical().getPhH2o() : null)
                .organicMatterPct(response.getChemical() != null ? response.getChemical().getOrganicMatterPct() : null)
                .nitrogenGKg(response.getChemical() != null ? response.getChemical().getNitrogenGKg() : null)
                .cecCmolKg(response.getChemical() != null ? response.getChemical().getCecCmolKg() : null)
                .capacityFieldVolPct(response.getWater() != null ? response.getWater().getCapacityFieldVolPct() : null)
                .capacityWiltVolPct(response.getWater() != null ? response.getWater().getCapacityWiltVolPct() : null)
                .latencySeconds(response.getMeta() != null ? response.getMeta().getLatencySeconds() : null)
                .build();
    }

    /**
     * Get soil suitability score for a crop based on soil properties
     * 
     * @param soilData Soil data
     * @param cropType Type of crop
     * @return Suitability score (0-100)
     */
    public Double calculateSoilSuitabilityScore(SoilDataDto soilData, String cropType) {
        if (soilData == null) {
            return 50.0; // Default neutral score
        }

        double score = 50.0;

        // pH suitability
        if (soilData.getPhH2o() != null) {
            double ph = soilData.getPhH2o();
            if (ph >= 6.0 && ph <= 7.5) {
                score += 15; // Optimal pH range
            } else if (ph >= 5.5 && ph <= 8.0) {
                score += 10; // Acceptable range
            } else {
                score -= 10; // Outside acceptable range
            }
        }

        // Organic matter suitability
        if (soilData.getOrganicMatterPct() != null) {
            if (soilData.getOrganicMatterPct() >= 2.5) {
                score += 15; // High organic matter
            } else if (soilData.getOrganicMatterPct() >= 1.5) {
                score += 10; // Medium organic matter
            } else {
                score += 5; // Low organic matter
            }
        }

        // Nitrogen availability
        if (soilData.getNitrogenGKg() != null) {
            if (soilData.getNitrogenGKg() >= 2.0) {
                score += 10; // High nitrogen
            } else if (soilData.getNitrogenGKg() >= 1.0) {
                score += 5; // Medium nitrogen
            }
        }

        // Water holding capacity
        if (soilData.getCapacityFieldVolPct() != null) {
            if (soilData.getCapacityFieldVolPct() >= 25) {
                score += 10; // Good water holding
            } else if (soilData.getCapacityFieldVolPct() >= 15) {
                score += 5; // Moderate water holding
            }
        }

        // Ensure score is within 0-100 range
        return Math.min(100.0, Math.max(0.0, score));
    }
}
