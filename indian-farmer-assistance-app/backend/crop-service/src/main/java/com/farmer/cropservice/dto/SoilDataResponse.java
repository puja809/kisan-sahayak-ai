package com.farmer.cropservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SoilDataResponse {
    @JsonProperty("soil_type")
    private SoilType soilType;
    
    @JsonProperty("physical_properties")
    private PhysicalProperties physicalProperties;
    
    @JsonProperty("chemical_properties")
    private ChemicalProperties chemicalProperties;
    
    @JsonProperty("water_metrics")
    private WaterMetrics waterMetrics;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SoilType {
        @JsonProperty("texture_class")
        private String textureClass;
        
        @JsonProperty("fao_class")
        private String faoClass;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhysicalProperties {
        @JsonProperty("sand_pct")
        private Double sandPct;
        
        @JsonProperty("silt_pct")
        private Double siltPct;
        
        @JsonProperty("clay_pct")
        private Double clayPct;
        
        @JsonProperty("bulk_density_g_cm3")
        private Double bulkDensityGCm3;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChemicalProperties {
        @JsonProperty("ph_h2o")
        private Double phH2o;
        
        @JsonProperty("organic_matter_pct")
        private Double organicMatterPct;
        
        @JsonProperty("nitrogen_g_kg")
        private Double nitrogenGKg;
        
        @JsonProperty("cec_cmol_kg")
        private Double cecCmolKg;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WaterMetrics {
        @JsonProperty("capacity_field_vol_pct")
        private Double capacityFieldVolPct;
        
        @JsonProperty("capacity_wilt_vol_pct")
        private Double capacityWiltVolPct;
    }
}
