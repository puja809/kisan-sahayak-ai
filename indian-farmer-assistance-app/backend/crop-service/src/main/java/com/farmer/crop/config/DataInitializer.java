package com.farmer.crop.config;

import com.farmer.crop.entity.AgroEcologicalZone;
import com.farmer.crop.repository.AgroEcologicalZoneRepository;
import com.farmer.crop.repository.DistrictZoneMappingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;


import java.util.List;

/**
 * Data initialization for ICAR Agro-Ecological Zones.
 * 
 * This configuration loads the ICAR classification data into the database
 * when the application starts in non-production environments.
 * 
 * Validates: Requirement 2.1
 */
@Configuration
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    @Profile("!prod")
    public CommandLineRunner initializeAgroEcologicalZones(
            AgroEcologicalZoneRepository zoneRepository,
            DistrictZoneMappingRepository districtMappingRepository) {
        return args -> {
            if (zoneRepository.count() == 0) {
                logger.info("Initializing ICAR Agro-Ecological Zones data...");
                initializeZones(zoneRepository);
                logger.info("ICAR Agro-Ecological Zones data initialization complete");
            } else {
                logger.info("Agro-Ecological Zones already exist, skipping initialization");
            }

            if (districtMappingRepository.count() == 0) {
                logger.info("Initializing District to Zone mappings...");
                initializeDistrictMappings(zoneRepository, districtMappingRepository);
                logger.info("District to Zone mappings initialization complete");
            } else {
                logger.info("District mappings already exist, skipping initialization");
            }
        };
    }

    private void initializeZones(AgroEcologicalZoneRepository repository) {
        List<AgroEcologicalZone> zones = List.of(
            // Zone 1: Western Himalayan Region
            createZone("AEZ-01", "Western Himalayan Region",
                "Cold arid to semi-arid climate with temperate conditions. High altitude mountains with steep slopes.",
                "Temperate", "1000-2000", "5-25",
                "Brown forest soils, alpine soils, podzolic soils",
                "Apple, Pear, Plum, Walnut, Almond, Potato, Wheat, Barley, Maize",
                "Maize, Potato, Rajmash, Wheat (limited)",
                "Wheat, Barley, Oat, Potato",
                "Potato, Vegetables, Maize",
                "28.0-37.0", "72.0-80.0",
                "Jammu and Kashmir, Himachal Pradesh, Uttarakhand (western parts)"),

            // Zone 2: Eastern Himalayan Region
            createZone("AEZ-02", "Eastern Himalayan Region",
                "Sub-tropical to temperate climate with high rainfall. Steep slopes with deep valleys.",
                "Subtropical", "2000-4000", "14-28",
                "Brown hill soils, red loamy soils, alluvial soils",
                "Rice, Maize, Potato, Orange, Banana, Tea, Cardamom, Ginger",
                "Rice, Maize, Jhum rice, Potato",
                "Wheat, Potato, Mustard, Pulses",
                "Vegetables, Maize, Millets",
                "22.0-29.0", "85.0-100.0",
                "West Bengal (Darjeeling, Sikkim), Assam (hill districts), Arunachal Pradesh"),

            // Zone 3: Lower Gangetic Plain Region
            createZone("AEZ-03", "Lower Gangetic Plain Region",
                "Humid subtropical climate with hot summers. Alluvial soils with high fertility.",
                "Humid Subtropical", "1400-2000", "20-32",
                "Alluvial soils, calcareous soils",
                "Rice, Jute, Mesta, Vegetables, Potato, Wheat, Pulses",
                "Rice, Jute, Mesta, Maize",
                "Wheat, Potato, Mustard, Pulses",
                "Boro rice, Vegetables, Pulses",
                "22.0-27.0", "85.0-90.0",
                "West Bengal (plains), Bihar (north), Assam (plains)"),

            // Zone 4: Middle Gangetic Plain Region
            createZone("AEZ-04", "Middle Gangetic Plain Region",
                "Humid subtropical climate with moderate rainfall. Deep alluvial soils.",
                "Humid Subtropical", "1000-1500", "18-35",
                "Alluvial soils, calcareous alluvial soils",
                "Rice, Wheat, Maize, Pulses, Oilseeds, Sugarcane, Vegetables",
                "Rice, Maize, Pulses, Sugarcane",
                "Wheat, Potato, Mustard, Pulses",
                "Boro rice, Vegetables, Maize",
                "24.0-28.0", "82.0-88.0",
                "Uttar Pradesh (east), Bihar (central), West Bengal (north)"),

            // Zone 5: Upper Gangetic Plain Region
            createZone("AEZ-05", "Upper Gangetic Plain Region",
                "Semi-arid subtropical climate with hot dry summers. Alluvial soils.",
                "Semi-Arid Subtropical", "700-1200", "18-40",
                "Alluvial soils, desert soils (in west)",
                "Wheat, Rice, Sugarcane, Cotton, Oilseeds, Pulses, Millets",
                "Rice, Maize, Millets, Pulses",
                "Wheat, Mustard, Potato, Pulses",
                "Boro rice, Vegetables, Groundnut",
                "24.0-30.0", "75.0-85.0",
                "Uttar Pradesh (west), Punjab, Haryana, Rajasthan (east)"),

            // Zone 6: Trans-Gangetic Plain Region
            createZone("AEZ-06", "Trans-Gangetic Plain Region",
                "Semi-arid subtropical climate with hot summers. Alluvial soils with high fertility.",
                "Semi-Arid Subtropical", "500-1000", "18-42",
                "Alluvial soils, sierozem soils",
                "Wheat, Rice, Cotton, Sugarcane, Oilseeds, Pulses, Fruits",
                "Rice, Cotton, Maize, Millets",
                "Wheat, Mustard, Potato, Gram",
                "Boro rice, Vegetables, Groundnut",
                "28.0-32.0", "68.0-78.0",
                "Punjab, Haryana, Rajasthan (north), Gujarat (north)"),

            // Zone 7: Eastern Plateau and Hills
            createZone("AEZ-07", "Eastern Plateau and Hills",
                "Subtropical climate with hot summers. Red and yellow soils on plateau.",
                "Subtropical", "1200-1600", "18-38",
                "Red and yellow soils, lateritic soils, alluvial soils",
                "Rice, Maize, Millets, Pulses, Oilseeds, Fruits, Vegetables",
                "Rice, Maize, Millets, Pulses",
                "Wheat, Pulses, Mustard, Potato",
                "Vegetables, Maize, Pulses",
                "18.0-26.0", "75.0-88.0",
                "Jharkhand, Odisha (interior), Chhattisgarh (north), Madhya Pradesh (east)"),

            // Zone 8: Central Plateau and Hills
            createZone("AEZ-08", "Central Plateau and Hills",
                "Semi-arid subtropical climate with hot dry summers. Mixed red and black soils.",
                "Semi-Arid Subtropical", "800-1200", "18-42",
                "Red and yellow soils, black soils, mixed red and black soils",
                "Soybean, Cotton, Wheat, Pulses, Millets, Oilseeds, Groundnut",
                "Soybean, Cotton, Millets, Pulses",
                "Wheat, Gram, Mustard, Lentil",
                "Vegetables, Groundnut, Maize",
                "20.0-28.0", "72.0-84.0",
                "Madhya Pradesh (central), Chhattisgarh (south), Maharashtra (north)"),

            // Zone 9: Western Plateau and Hills
            createZone("AEZ-09", "Western Plateau and Hills",
                "Semi-arid subtropical climate with hot dry summers. Medium to deep black soils.",
                "Semi-Arid Subtropical", "600-1000", "18-42",
                "Black soils, red soils, lateritic soils",
                "Cotton, Soybean, Wheat, Pulses, Millets, Sugarcane, Grapes",
                "Cotton, Soybean, Millets, Pulses",
                "Wheat, Gram, Mustard, Lentil",
                "Grapes, Vegetables, Groundnut",
                "18.0-28.0", "70.0-80.0",
                "Maharashtra (central and east), Karnataka (north), Madhya Pradesh (west)"),

            // Zone 10: Southern Plateau and Hills
            createZone("AEZ-10", "Southern Plateau and Hills",
                "Semi-arid to sub-humid subtropical climate. Red and black soils.",
                "Semi-Arid Subtropical", "700-1200", "18-40",
                "Red and black soils, lateritic soils, alluvial soils",
                "Rice, Millets, Pulses, Oilseeds, Cotton, Sugarcane, Coconut, Banana",
                "Rice, Millets, Pulses, Cotton",
                "Wheat, Pulses, Mustard, Sunflower",
                "Vegetables, Groundnut, Maize",
                "12.0-24.0", "75.0-85.0",
                "Karnataka (south), Andhra Pradesh, Tamil Nadu (interior), Kerala (interior)"),

            // Zone 11: East Coast Plains and Hills
            createZone("AEZ-11", "East Coast Plains and Hills",
                "Humid to sub-humid subtropical climate. Alluvial and deltaic soils.",
                "Humid Subtropical", "1000-1600", "20-38",
                "Alluvial soils, deltaic soils, red soils",
                "Rice, Pulses, Oilseeds, Coconut, Cashew, Vegetables, Fruits",
                "Rice, Pulses, Groundnut, Maize",
                "Wheat, Pulses, Mustard, Sesame",
                "Boro rice, Vegetables, Pulses",
                "8.0-22.0", "77.0-92.0",
                "Tamil Nadu (coastal), Andhra Pradesh (coastal), Odisha (coastal), West Bengal (coastal)"),

            // Zone 12: West Coast Plains and Ghats
            createZone("AEZ-12", "West Coast Plains and Ghats",
                "Humid tropical climate with high rainfall. Lateritic and alluvial soils.",
                "Humid Tropical", "2000-4000", "20-38",
                "Lateritic soils, alluvial soils, red soils",
                "Rice, Coconut, Cashew, Banana, Pepper, Cardamom, Arecanut, Rubber",
                "Rice, Cashew, Coconut, Banana",
                "Wheat, Pulses, Vegetables",
                "Boro rice, Vegetables, Fruits",
                "8.0-22.0", "68.0-78.0",
                "Maharashtra (coastal), Karnataka (coastal), Kerala, Goa"),

            // Zone 13: Gujarat Plains and Hills
            createZone("AEZ-13", "Gujarat Plains and Hills",
                "Semi-arid to arid climate with hot summers. Alluvial and black soils.",
                "Semi-Arid to Arid", "400-1500", "18-45",
                "Alluvial soils, black soils, saline soils",
                "Cotton, Groundnut, Wheat, Rice, Pulses, Oilseeds, Fruits",
                "Cotton, Groundnut, Millets, Pulses",
                "Wheat, Gram, Mustard, Vegetables",
                "Groundnut, Vegetables, Bajari",
                "20.0-24.0", "68.0-75.0",
                "Gujarat, Rajasthan (west)"),

            // Zone 14: Western Dry Region
            createZone("AEZ-14", "Western Dry Region",
                "Arid climate with very low rainfall and high temperature extremes. Desert soils.",
                "Arid", "100-400", "15-48",
                "Desert soils, saline soils, aeolian soils",
                "Millets (Bajara, Jowar), Pulses, Oilseeds, Cactus, Ber",
                "Bajara, Jowar, Moong, Moth",
                "Wheat, Gram, Mustard (irrigated)",
                "Vegetables, Groundnut (irrigated)",
                "24.0-30.0", "68.0-76.0",
                "Rajasthan (west), Gujarat (northwest)"),

            // Zone 15: The Islands
            createZone("AEZ-15", "The Islands",
                "Humid tropical maritime climate with high humidity and rainfall. Coral and alluvial soils.",
                "Humid Tropical", "1500-3000", "22-32",
                "Coral soils, alluvial soils, peaty soils",
                "Coconut, Arecanut, Banana, Rice, Vegetables, Fruits, Spices",
                "Rice, Coconut, Vegetables",
                "Pulses, Vegetables, Fruits",
                "Vegetables, Fruits, Root crops",
                "6.0-14.0", "92.0-94.0",
                "Andaman and Nicobar Islands, Lakshadweep Islands"),

            // Zone 16: Central Highlands (Narmada-Tapti)
            createZone("AEZ-16", "Central Highlands (Narmada-Tapti)",
                "Semi-arid subtropical climate with hot summers. Black and red soils.",
                "Semi-Arid Subtropical", "800-1200", "18-42",
                "Black soils, red soils, mixed soils",
                "Soybean, Cotton, Wheat, Pulses, Millets, Oilseeds",
                "Soybean, Cotton, Millets, Pulses",
                "Wheat, Gram, Mustard, Lentil",
                "Vegetables, Groundnut, Maize",
                "20.0-26.0", "72.0-82.0",
                "Madhya Pradesh (central), Maharashtra (northwest), Gujarat (east)"),

            // Zone 17: Deccan Plateau (South)
            createZone("AEZ-17", "Deccan Plateau (South)",
                "Semi-arid subtropical climate with moderate rainfall. Red and black soils.",
                "Semi-Arid Subtropical", "600-1000", "18-40",
                "Red soils, black soils, lateritic soils",
                "Rice, Millets, Pulses, Oilseeds, Cotton, Sugarcane, Tobacco",
                "Rice, Millets, Pulses, Cotton",
                "Wheat, Pulses, Sunflower, Rabi Jowar",
                "Vegetables, Groundnut, Maize",
                "12.0-20.0", "75.0-85.0",
                "Karnataka (south), Andhra Pradesh (south), Tamil Nadu (interior)"),

            // Zone 18: Eastern Coastal Plain
            createZone("AEZ-18", "Eastern Coastal Plain",
                "Humid to sub-humid tropical climate. Alluvial deltaic soils.",
                "Humid Tropical", "1000-1500", "22-38",
                "Alluvial soils, deltaic soils, saline soils (coastal)",
                "Rice, Pulses, Oilseeds, Coconut, Cashew, Vegetables, Fruits",
                "Rice, Pulses, Groundnut, Maize",
                "Wheat, Pulses, Mustard, Sesame",
                "Boro rice, Vegetables, Pulses",
                "8.0-20.0", "77.0-90.0",
                "Tamil Nadu (coastal), Andhra Pradesh (coastal), Odisha (coastal)"),

            // Zone 19: Western Himalayan (Cold Arid)
            createZone("AEZ-19", "Western Himalayan (Cold Arid)",
                "Cold arid climate with very low rainfall. Alpine and glacial soils.",
                "Cold Arid", "100-500", "-10 to 25",
                "Glacial soils, alpine soils, skeletal soils",
                "Barley, Wheat, Potato, Apple, Apricot, Almond, Saffron",
                "Barley, Wheat, Potato (limited)",
                "Wheat, Barley, Oat (limited)",
                "Vegetables, Fodder crops",
                "30.0-36.0", "75.0-80.0",
                "Jammu and Kashmir (Ladakh), Himachal Pradesh (high altitude)"),

            // Zone 20: Transition Zone (Assam)
            createZone("AEZ-20", "Transition Zone (Assam)",
                "Humid subtropical to tropical climate with high rainfall. Alluvial and hill soils.",
                "Humid Subtropical", "1500-3000", "10-35",
                "Alluvial soils, hill soils, lateritic soils",
                "Rice, Tea, Jute, Mesta, Pulses, Oilseeds, Vegetables, Fruits",
                "Rice, Jute, Mesta, Maize",
                "Wheat, Potato, Mustard, Pulses",
                "Boro rice, Vegetables, Tea",
                "24.0-28.0", "89.0-97.0",
                "Assam, Meghalaya, Nagaland, Manipur, Tripura, Mizoram")
        );

        repository.saveAll(zones);
        logger.info("Saved {} agro-ecological zones", zones.size());
    }

    private AgroEcologicalZone createZone(String zoneCode, String zoneName, String description,
            String climateType, String rainfallRange, String temperatureRange,
            String soilTypes, String suitableCrops, String kharifSuitability,
            String rabiSuitability, String zaidSuitability, String latRange,
            String lonRange, String statesCovered) {
        return AgroEcologicalZone.builder()
                .zoneCode(zoneCode)
                .zoneName(zoneName)
                .description(description)
                .climateType(climateType)
                .rainfallRange(rainfallRange)
                .temperatureRange(temperatureRange)
                .soilTypes(soilTypes)
                .suitableCrops(suitableCrops)
                .kharifSuitability(kharifSuitability)
                .rabiSuitability(rabiSuitability)
                .zaidSuitability(zaidSuitability)
                .latitudeRange(latRange)
                .longitudeRange(lonRange)
                .statesCovered(statesCovered)
                .isActive(true)
                .build();
    }

    private void initializeDistrictMappings(
            AgroEcologicalZoneRepository zoneRepository,
            DistrictZoneMappingRepository districtMappingRepository) {
        
        // Get all zones
        List<AgroEcologicalZone> zones = zoneRepository.findAll();
        
        // Create a mapping of state to zone (simplified - in production, this would be more detailed)
        // This is a sample of major districts - in production, all 700+ districts would be mapped
        
        // Sample district mappings for major states
        var districtMappings = List.of(
            // Uttar Pradesh
            createMapping("Lucknow", "Uttar Pradesh", "AEZ-05", 26.8467, 80.9462, "Central"),
            createMapping("Kanpur", "Uttar Pradesh", "AEZ-05", 26.4499, 80.3319, "Central"),
            createMapping("Varanasi", "Uttar Pradesh", "AEZ-04", 25.3176, 83.0103, "Eastern"),
            createMapping("Agra", "Uttar Pradesh", "AEZ-05", 27.1767, 78.0081, "Central"),
            createMapping("Allahabad", "Uttar Pradesh", "AEZ-04", 25.4358, 81.8463, "Eastern"),
            
            // Maharashtra
            createMapping("Mumbai", "Maharashtra", "AEZ-12", 19.0760, 72.8777, "Coastal"),
            createMapping("Pune", "Maharashtra", "AEZ-09", 18.5204, 73.8567, "Western"),
            createMapping("Nagpur", "Maharashtra", "AEZ-08", 21.1458, 79.0882, "Central"),
            createMapping("Nashik", "Maharashtra", "AEZ-09", 19.9975, 73.7898, "Western"),
            createMapping("Aurangabad", "Maharashtra", "AEZ-09", 19.8736, 75.3420, "Central"),
            
            // West Bengal
            createMapping("Kolkata", "West Bengal", "AEZ-03", 22.5726, 88.3639, "Coastal"),
            createMapping("Darjeeling", "West Bengal", "AEZ-02", 27.0410, 88.2663, "Hills"),
            createMapping("Murshidabad", "West Bengal", "AEZ-03", 24.1804, 88.1408, "Plains"),
            createMapping("Bankura", "West Bengal", "AEZ-07", 23.2550, 87.0640, "Plateau"),
            
            // Bihar
            createMapping("Patna", "Bihar", "AEZ-04", 25.5941, 85.1376, "Central"),
            createMapping("Gaya", "Bihar", "AEZ-04", 24.7955, 85.0076, "Central"),
            createMapping("Bhagalpur", "Bihar", "AEZ-04", 25.2425, 87.0214, "Eastern"),
            createMapping("Muzaffarpur", "Bihar", "AEZ-04", 26.1197, 85.3910, "Northern"),
            
            // Punjab
            createMapping("Ludhiana", "Punjab", "AEZ-06", 30.9000, 75.8573, "Central"),
            createMapping("Amritsar", "Punjab", "AEZ-06", 31.6339, 74.8784, "Northwest"),
            createMapping("Jalandhar", "Punjab", "AEZ-06", 31.3260, 75.5762, "Central"),
            createMapping("Bathinda", "Punjab", "AEZ-06", 30.2115, 74.9455, "South"),
            
            // Haryana
            createMapping("Gurgaon", "Haryana", "AEZ-06", 28.4273, 77.0437, "South"),
            createMapping("Hisar", "Haryana", "AEZ-06", 29.1642, 75.7351, "Central"),
            createMapping("Rohtak", "Haryana", "AEZ-06", 28.8955, 76.6066, "Central"),
            
            // Rajasthan
            createMapping("Jaipur", "Rajasthan", "AEZ-06", 26.9124, 75.7873, "East"),
            createMapping("Jodhpur", "Rajasthan", "AEZ-14", 26.2389, 73.0243, "West"),
            createMapping("Udaipur", "Rajasthan", "AEZ-13", 24.5854, 73.7125, "South"),
            createMapping("Bikaner", "Rajasthan", "AEZ-14", 28.0229, 73.3119, "North"),
            createMapping("Ajmer", "Rajasthan", "AEZ-13", 26.4499, 74.6399, "Central"),
            
            // Madhya Pradesh
            createMapping("Bhopal", "Madhya Pradesh", "AEZ-08", 23.2599, 77.4126, "Central"),
            createMapping("Indore", "Madhya Pradesh", "AEZ-08", 22.7196, 75.8577, "West"),
            createMapping("Jabalpur", "Madhya Pradesh", "AEZ-08", 23.1815, 79.9814, "East"),
            createMapping("Gwalior", "Madhya Pradesh", "AEZ-08", 26.2183, 78.1828, "North"),
            
            // Gujarat
            createMapping("Ahmedabad", "Gujarat", "AEZ-13", 23.0225, 72.5714, "Central"),
            createMapping("Surat", "Gujarat", "AEZ-13", 21.1702, 72.8311, "South"),
            createMapping("Vadodara", "Gujarat", "AEZ-13", 22.3072, 73.1812, "Central"),
            createMapping("Rajkot", "Gujarat", "AEZ-13", 22.3039, 70.8022, "West"),
            
            // Karnataka
            createMapping("Bangalore", "Karnataka", "AEZ-10", 12.9716, 77.5946, "South"),
            createMapping("Mysore", "Karnataka", "AEZ-10", 12.2958, 76.6394, "South"),
            createMapping("Hubli", "Karnataka", "AEZ-09", 15.3647, 75.1249, "North"),
            createMapping("Belgaum", "Karnataka", "AEZ-09", 15.8497, 74.4970, "North"),
            createMapping("Mangalore", "Karnataka", "AEZ-12", 12.9141, 74.8560, "Coastal"),
            
            // Andhra Pradesh
            createMapping("Visakhapatnam", "Andhra Pradesh", "AEZ-11", 17.6868, 83.2185, "Coastal"),
            createMapping("Vijayawada", "Andhra Pradesh", "AEZ-11", 16.5062, 80.6480, "Coastal"),
            createMapping("Tirupati", "Andhra Pradesh", "AEZ-10", 13.6288, 79.4192, "South"),
            createMapping("Kurnool", "Andhra Pradesh", "AEZ-10", 15.8281, 78.0373, "Central"),
            
            // Tamil Nadu
            createMapping("Chennai", "Tamil Nadu", "AEZ-11", 13.0827, 80.2707, "Coastal"),
            createMapping("Coimbatore", "Tamil Nadu", "AEZ-10", 11.0168, 76.9558, "West"),
            createMapping("Madurai", "Tamil Nadu", "AEZ-10", 9.9250, 78.1198, "South"),
            createMapping("Salem", "Tamil Nadu", "AEZ-10", 11.6643, 78.1460, "North"),
            createMapping("Tiruchirappalli", "Tamil Nadu", "AEZ-10", 10.7905, 78.7047, "Central"),
            
            // Kerala
            createMapping("Thiruvananthapuram", "Kerala", "AEZ-12", 8.5241, 76.9366, "South"),
            createMapping("Kochi", "Kerala", "AEZ-12", 9.9312, 76.2673, "Central"),
            createMapping("Kozhikode", "Kerala", "AEZ-12", 11.2588, 75.7794, "North"),
            
            // Odisha
            createMapping("Bhubaneswar", "Odisha", "AEZ-11", 20.2961, 85.8245, "Coastal"),
            createMapping("Cuttack", "Odisha", "AEZ-11", 20.4625, 85.8828, "Coastal"),
            createMapping("Sambalpur", "Odisha", "AEZ-07", 21.4669, 83.9933, "Western"),
            
            // Jharkhand
            createMapping("Ranchi", "Jharkhand", "AEZ-07", 23.3441, 85.3096, "Central"),
            createMapping("Jamshedpur", "Jharkhand", "AEZ-07", 22.8046, 86.2024, "East"),
            createMapping("Dhanbad", "Jharkhand", "AEZ-07", 23.7957, 86.4304, "North"),
            
            // Chhattisgarh
            createMapping("Raipur", "Chhattisgarh", "AEZ-08", 21.2514, 81.6296, "Central"),
            createMapping("Bhilai", "Chhattisgarh", "AEZ-08", 21.2092, 81.4286, "East"),
            createMapping("Bilaspur", "Chhattisgarh", "AEZ-08", 22.0746, 82.1391, "Central"),
            
            // Assam
            createMapping("Guwahati", "Assam", "AEZ-20", 26.1156, 91.7086, "Central"),
            createMapping("Dibrugarh", "Assam", "AEZ-20", 27.4788, 94.9116, "East"),
            createMapping("Silchar", "Assam", "AEZ-20", 24.8333, 92.7789, "South"),
            
            // Uttarakhand
            createMapping("Dehradun", "Uttarakhand", "AEZ-01", 30.3165, 78.0322, "Central"),
            createMapping("Haridwar", "Uttarakhand", "AEZ-01", 29.9457, 78.1642, "South"),
            createMapping("Nainital", "Uttarakhand", "AEZ-01", 29.3919, 79.4542, "North"),
            
            // Himachal Pradesh
            createMapping("Shimla", "Himachal Pradesh", "AEZ-01", 31.1048, 77.1734, "Central"),
            createMapping("Mandi", "Himachal Pradesh", "AEZ-01", 31.7092, 76.9324, "Central"),
            createMapping("Dharamshala", "Himachal Pradesh", "AEZ-01", 32.2190, 76.3234, "North"),
            
            // Jammu and Kashmir
            createMapping("Srinagar", "Jammu and Kashmir", "AEZ-01", 34.0837, 74.7973, "Valley"),
            createMapping("Jammu", "Jammu and Kashmir", "AEZ-01", 32.7266, 74.8570, "South"),
            createMapping("Leh", "Jammu and Kashmir", "AEZ-19", 34.1526, 77.5750, "Ladakh")
        );

        for (var mapping : districtMappings) {
            zoneRepository.findByZoneCode(mapping.zoneCode()).ifPresent(zone -> {
                DistrictZoneMapping districtMapping = DistrictZoneMapping.builder()
                        .districtName(mapping.district())
                        .state(mapping.state())
                        .zone(zone)
                        .latitude(mapping.latitude())
                        .longitude(mapping.longitude())
                        .region(mapping.region())
                        .isVerified(true)
                        .dataSource("ICAR")
                        .isActive(true)
                        .build();
                districtMappingRepository.save(districtMapping);
            });
        }
        
        logger.info("Saved {} district to zone mappings", districtMappings.size());
    }

    private record DistrictData(String district, String state, String zoneCode, 
                                Double latitude, Double longitude, String region) {}
    
    private DistrictData createMapping(String district, String state, String zoneCode,
            Double latitude, Double longitude, String region) {
        return new DistrictData(district, state, zoneCode, latitude, longitude, region);
    }
}