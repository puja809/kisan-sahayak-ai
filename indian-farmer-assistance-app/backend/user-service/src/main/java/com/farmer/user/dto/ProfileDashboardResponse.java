package com.farmer.user.dto;

import lombok.*;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for farmer profile dashboard aggregation.
 * Requirements: 11A.8
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileDashboardResponse {

    // User summary
    private UserResponse user;
    
    // Farm summary
    private FarmSummary farmSummary;
    
    // Current crops
    private List<CropResponse> currentCrops;
    
    // Upcoming activities
    private List<UpcomingActivity> upcomingActivities;
    
    // Financial summary
    private FinancialSummary financialSummary;
    
    // Recent changes
    private List<RecentChange> recentChanges;
    
    // Livestock summary
    private LivestockSummary livestockSummary;
    
    // Equipment summary
    private EquipmentSummary equipmentSummary;
    
    /**
     * Farm summary statistics.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FarmSummary {
        private Integer totalParcels;
        private Double totalAreaAcres;
        private String primarySoilType;
        private String primaryIrrigationType;
    }

    /**
     * Upcoming activity for the farmer.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpcomingActivity {
        private String activityType;
        private String description;
        private LocalDate dueDate;
        private String cropName;
        private String priority;
    }

    /**
     * Financial summary for the farmer.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FinancialSummary {
        private Double totalInputCosts;
        private Double totalRevenue;
        private Double profitLoss;
        private Double profitMargin;
        private Double currentSeasonInputCosts;
        private Double currentSeasonRevenue;
        private Double currentSeasonProfitLoss;
    }

    /**
     * Recent change in profile.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentChange {
        private String changeType;
        private String entityType;
        private String description;
        private LocalDateTime timestamp;
    }

    /**
     * Livestock summary statistics.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LivestockSummary {
        private Integer totalLivestock;
        private Integer cattleCount;
        private Integer buffaloCount;
        private Integer goatCount;
        private Integer sheepCount;
        private Integer poultryCount;
    }

    /**
     * Equipment summary statistics.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EquipmentSummary {
        private Integer totalEquipment;
        private Integer tractors;
        private Integer harvesters;
        private Integer pumpSets;
        private Integer requiringMaintenance;
        private Double totalValue;
    }
}