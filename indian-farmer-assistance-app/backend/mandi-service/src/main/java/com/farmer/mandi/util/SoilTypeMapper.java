package com.farmer.mandi.util;

import java.util.HashMap;
import java.util.Map;

public class SoilTypeMapper {
    
    private static final Map<String, String> KAEGRO_TO_ML_MAPPING = new HashMap<>();
    
    static {
        // Map Kaegro soil types to ML model soil types
        KAEGRO_TO_ML_MAPPING.put("Sand", "sandy");
        KAEGRO_TO_ML_MAPPING.put("Loamy Sand", "sandy_loam");
        KAEGRO_TO_ML_MAPPING.put("Sandy Loam", "sandy_loam");
        KAEGRO_TO_ML_MAPPING.put("Loam", "loamy");
        KAEGRO_TO_ML_MAPPING.put("Silt Loam", "loamy");
        KAEGRO_TO_ML_MAPPING.put("Silt", "loamy");
        KAEGRO_TO_ML_MAPPING.put("Sandy Clay Loam", "sandy_loam");
        KAEGRO_TO_ML_MAPPING.put("Clay Loam", "clay");
        KAEGRO_TO_ML_MAPPING.put("Silty Clay Loam", "silt_clay");
        KAEGRO_TO_ML_MAPPING.put("Sandy Clay", "clay");
        KAEGRO_TO_ML_MAPPING.put("Silty Clay", "silt_clay");
        KAEGRO_TO_ML_MAPPING.put("Clay", "clay");
    }
    
    public static String mapKaegrToML(String kaegrSoilType) {
        if (kaegrSoilType == null || kaegrSoilType.trim().isEmpty()) {
            return "loamy"; // Default fallback
        }
        
        String mapped = KAEGRO_TO_ML_MAPPING.get(kaegrSoilType.trim());
        return mapped != null ? mapped : "loamy"; // Default fallback
    }
}
