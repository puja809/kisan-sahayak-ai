package com.farmer.crop.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Crop family classification for rotation analysis.
 * Crops in the same family share similar nutrient requirements and pest profiles.
 * 
 * Requirements: 3.1, 3.2
 */
public enum CropFamily {
    /**
     * Cereal grains - high nitrogen consumers
     */
    CEREALS("Cereals", RootDepth.DEEP, 
            "Rice", "Wheat", "Maize", "Barley", "Sorghum", "Millet", "Ragi", "Bajra", "Paddy"),
    
    /**
     * Legumes - nitrogen fixers
     */
    LEGUMES("Legumes", RootDepth.MEDIUM,
            "Greengram", "Blackgram", "Redgram", "Chickpea", "Lentil", "Peas", 
            "Soybean", "Groundnut", "Cowpea", "Horsegram", "Mothbean"),
    
    /**
     * Brassicas - heavy potassium consumers
     */
    BRASSICAS("Brassicas", RootDepth.SHALLOW,
            "Cabbage", "Cauliflower", "Broccoli", "Kale", "Mustard", "Rapeseed", 
            "Turnip", "Radish", "Knol-khol"),
    
    /**
     * Solanaceous crops - moderate nutrient consumers
     */
    SOLANACEOUS("Solanaceous", RootDepth.MEDIUM,
            "Tomato", "Potato", "Brinjal", "Chili", "Bell Pepper", "Tobacco"),
    
    /**
     * Cucurbits - shallow rooted, moderate consumers
     */
    CUCURBITS("Cucurbits", RootDepth.SHALLOW,
            "Cucumber", "Bottle Gourd", "Bitter Gourd", "Pumpkin", "Squash", 
            "Melon", "Watermelon", "Zucchini"),
    
    /**
     * Root and tuber crops
     */
    ROOT_TUBERS("Root Tubers", RootDepth.DEEP,
            "Carrot", "Beetroot", "Onion", "Garlic", "Sweet Potato", "Tapioca", "Yam"),
    
    /**
     * Fiber crops
     */
    FIBER("Fiber", RootDepth.DEEP,
            "Cotton", "Jute", "Mesta", "Sisal", "Hemp"),
    
    /**
     * Oilseed crops
     */
    OILSEEDS("Oilseeds", RootDepth.MEDIUM,
            "Sunflower", "Sesame", "Niger", "Safflower", "Castor", "Linseed"),
    
    /**
     * Spice crops
     */
    SPICES("Spices", RootDepth.MEDIUM,
            "Coriander", "Cumin", "Fenugreek", "Turmeric", "Ginger", "Cardamom", 
            "Black Pepper", "Cinnamon", "Cloves"),
    
    /**
     * Fruit crops (typically perennial, but included for completeness)
     */
    FRUITS("Fruits", RootDepth.DEEP,
            "Mango", "Banana", "Citrus", "Papaya", "Guava", "Pomegranate", "Grapes"),
    
    /**
     * Green manure crops
     */
    GREEN_MANURE("Green Manure", RootDepth.MEDIUM,
            "Sesbania", "Crotalaria", "Sunhemp", "Dhaincha", "Glycine"),
    
    /**
     * Fodder crops
     */
    FODDER("Fodder", RootDepth.SHALLOW,
            "Berseem", "Lucerne", "Napier", "Sorghum Fodder", "Maize Fodder");

    private final String familyName;
    private final RootDepth typicalRootDepth;
    private final List<String> commonCrops;

    private static final Map<String, CropFamily> CROP_TO_FAMILY_MAP = new HashMap<>();
    private static final Map<String, RootDepth> CROP_ROOT_DEPTH_MAP = new HashMap<>();

    static {
        for (CropFamily family : values()) {
            for (String crop : family.commonCrops) {
                CROP_TO_FAMILY_MAP.put(crop.toLowerCase(), family);
                CROP_ROOT_DEPTH_MAP.put(crop.toLowerCase(), family.typicalRootDepth);
            }
        }
    }

    CropFamily(String familyName, RootDepth typicalRootDepth, String... commonCrops) {
        this.familyName = familyName;
        this.typicalRootDepth = typicalRootDepth;
        this.commonCrops = Arrays.asList(commonCrops);
    }

    /**
     * Get the crop family for a given crop name.
     * 
     * @param cropName The name of the crop
     * @return The crop family, or null if not found
     */
    public static CropFamily getFamilyForCrop(String cropName) {
        if (cropName == null) {
            return null;
        }
        return CROP_TO_FAMILY_MAP.get(cropName.toLowerCase().trim());
    }

    /**
     * Get the typical root depth for a given crop.
     * 
     * @param cropName The name of the crop
     * @return The typical root depth
     */
    public static RootDepth getRootDepthForCrop(String cropName) {
        if (cropName == null) {
            return RootDepth.MEDIUM; // Default
        }
        RootDepth depth = CROP_ROOT_DEPTH_MAP.get(cropName.toLowerCase().trim());
        return depth != null ? depth : RootDepth.MEDIUM;
    }

    /**
     * Check if two crops belong to the same family.
     * 
     * @param crop1 First crop name
     * @param crop2 Second crop name
     * @return true if both crops are in the same family
     */
    public static boolean areInSameFamily(String crop1, String crop2) {
        CropFamily family1 = getFamilyForCrop(crop1);
        CropFamily family2 = getFamilyForCrop(crop2);
        return family1 != null && family1.equals(family2);
    }

    public String getFamilyName() {
        return familyName;
    }

    public RootDepth getTypicalRootDepth() {
        return typicalRootDepth;
    }

    public List<String> getCommonCrops() {
        return commonCrops;
    }

    /**
     * Root depth classification for nutrient cycling analysis.
     * Deep-rooted crops access nutrients from deeper soil layers.
     * Shallow-rooted crops deplete nutrients from the topsoil.
     * 
     * Requirements: 3.2, 3.3
     */
    public enum RootDepth {
        SHALLOW("Shallow", 30, "Topsoil nutrient depletion risk"),
        MEDIUM("Medium", 60, "Balanced nutrient uptake"),
        DEEP("Deep", 120, "Nutrient cycling from deeper layers");

        private final String description;
        private final int typicalDepthCm;
        private final String nutrientImpact;

        RootDepth(String description, int typicalDepthCm, String nutrientImpact) {
            this.description = description;
            this.typicalDepthCm = typicalDepthCm;
            this.nutrientImpact = nutrientImpact;
        }

        public String getDescription() {
            return description;
        }

        public int getTypicalDepthCm() {
            return typicalDepthCm;
        }

        public String getNutrientImpact() {
            return nutrientImpact;
        }
    }
}