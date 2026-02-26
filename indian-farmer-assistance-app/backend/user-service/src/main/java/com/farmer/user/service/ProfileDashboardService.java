package com.farmer.user.service;

import com.farmer.user.dto.*;
import com.farmer.user.entity.*;
import com.farmer.user.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for farmer profile dashboard aggregation.
 * Requirements: 11A.8
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileDashboardService {

    private final UserRepository userRepository;
    private final FarmRepository farmRepository;
    private final CropRepository cropRepository;
    private final FertilizerApplicationRepository fertilizerApplicationRepository;
    private final LivestockRepository livestockRepository;
    private final EquipmentRepository equipmentRepository;
    private final ProfileVersionRepository profileVersionRepository;

    /**
     * Get complete dashboard data for a farmer.
     * Requirements: 11A.8
     */
    @Transactional(readOnly = true)
    public ProfileDashboardResponse getDashboard(String farmerId) {
        log.info("Fetching dashboard for farmer: {}", farmerId);

        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new UserService.UserNotFoundException("User not found: " + farmerId));

        Long userId = user.getId();

        // Build dashboard response
        return ProfileDashboardResponse.builder()
                .user(UserResponse.fromEntity(user))
                .farmSummary(buildFarmSummary(userId))
                .currentCrops(getCurrentCrops(userId))
                .upcomingActivities(buildUpcomingActivities(userId))
                .financialSummary(buildFinancialSummary(userId))
                .recentChanges(buildRecentChanges(userId))
                .livestockSummary(buildLivestockSummary(userId))
                .equipmentSummary(buildEquipmentSummary(userId))
                .build();
    }

    /**
     * Build farm summary statistics.
     * Requirements: 11A.3, 11A.10
     */
    private ProfileDashboardResponse.FarmSummary buildFarmSummary(Long userId) {
        List<Farm> farms = farmRepository.findByUserIdAndIsActiveTrue(userId);

        if (farms.isEmpty()) {
            return ProfileDashboardResponse.FarmSummary.builder()
                    .totalParcels(0)
                    .totalAreaAcres(0.0)
                    .build();
        }

        // Calculate totals
        int totalParcels = farms.size();
        double totalArea = farms.stream()
                .mapToDouble(f -> f.getTotalAreaAcres() != null ? f.getTotalAreaAcres() : 0.0)
                .sum();

        // Find most common soil type
        String primarySoilType = farms.stream()
                .filter(f -> f.getSoilType() != null)
                .collect(Collectors.groupingBy(Farm::getSoilType, Collectors.counting()))
                .entrySet().stream()
                .max((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
                .map(e -> e.getKey())
                .orElse(null);

        // Find most common irrigation type
        String primaryIrrigationType = farms.stream()
                .filter(f -> f.getIrrigationType() != null)
                .collect(Collectors.groupingBy(Farm::getIrrigationType, Collectors.counting()))
                .entrySet().stream()
                .max((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
                .map(e -> e.getKey().name())
                .orElse(null);

        return ProfileDashboardResponse.FarmSummary.builder()
                .totalParcels(totalParcels)
                .totalAreaAcres(totalArea)
                .primarySoilType(primarySoilType)
                .primaryIrrigationType(primaryIrrigationType)
                .build();
    }

    /**
     * Get current crops for a user.
     * Requirements: 11A.4, 11A.8
     */
    private List<CropResponse> getCurrentCrops(Long userId) {
        return cropRepository.findCurrentCropsByUserId(userId).stream()
                .map(CropResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Build upcoming activities list.
     * Requirements: 11A.8
     */
    private List<ProfileDashboardResponse.UpcomingActivity> buildUpcomingActivities(Long userId) {
        List<ProfileDashboardResponse.UpcomingActivity> activities = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate nextMonth = today.plusMonths(1);

        // Get crops with upcoming harvest dates
        List<Crop> upcomingHarvests = cropRepository.findUpcomingHarvests(userId, today, nextMonth);
        for (Crop crop : upcomingHarvests) {
            activities.add(ProfileDashboardResponse.UpcomingActivity.builder()
                    .activityType("HARVEST")
                    .description("Expected harvest for " + crop.getCropName())
                    .dueDate(crop.getExpectedHarvestDate())
                    .cropName(crop.getCropName())
                    .priority("HIGH")
                    .build());
        }

        // Get crops that need fertilizer (growing crops without recent applications)
        List<Crop> growingCrops = cropRepository.findByFarmUserIdAndStatus(userId, Crop.CropStatus.GROWING);
        for (Crop crop : growingCrops) {
            long applicationCount = fertilizerApplicationRepository.countByCropId(crop.getId());
            if (applicationCount == 0) {
                activities.add(ProfileDashboardResponse.UpcomingActivity.builder()
                        .activityType("FERTILIZER")
                        .description("First fertilizer application for " + crop.getCropName())
                        .dueDate(crop.getSowingDate().plusDays(30))
                        .cropName(crop.getCropName())
                        .priority("MEDIUM")
                        .build());
            }
        }

        // Sort by due date
        activities.sort((a, b) -> {
            if (a.getDueDate() == null) return 1;
            if (b.getDueDate() == null) return -1;
            return a.getDueDate().compareTo(b.getDueDate());
        });

        return activities;
    }

    /**
     * Build financial summary.
     * Requirements: 11A.8
     */
    private ProfileDashboardResponse.FinancialSummary buildFinancialSummary(Long userId) {
        LocalDate yearStart = LocalDate.now().withDayOfYear(1);
        LocalDate today = LocalDate.now();

        // Year-to-date totals
        Double totalInputCosts = cropRepository.calculateTotalInputCost(userId, yearStart, today);
        Double totalRevenue = cropRepository.calculateTotalRevenue(userId, yearStart, today);

        Double inputCosts = totalInputCosts != null ? totalInputCosts : 0.0;
        Double revenue = totalRevenue != null ? totalRevenue : 0.0;
        Double profitLoss = revenue - inputCosts;

        Double profitMargin = inputCosts > 0.0
                ? (profitLoss / inputCosts) * 100.0
                : 0.0;

        // Current season (simplified - using last 6 months)
        LocalDate seasonStart = today.minusMonths(6);
        Double seasonInputCosts = cropRepository.calculateTotalInputCost(userId, seasonStart, today);
        Double seasonRevenue = cropRepository.calculateTotalRevenue(userId, seasonStart, today);

        Double seasonCosts = seasonInputCosts != null ? seasonInputCosts : 0.0;
        Double seasonRevenueBd = seasonRevenue != null ? seasonRevenue : 0.0;
        Double seasonProfitLoss = seasonRevenueBd - seasonCosts;

        return ProfileDashboardResponse.FinancialSummary.builder()
                .totalInputCosts(inputCosts)
                .totalRevenue(revenue)
                .profitLoss(profitLoss)
                .profitMargin(profitMargin)
                .currentSeasonInputCosts(seasonCosts)
                .currentSeasonRevenue(seasonRevenueBd)
                .currentSeasonProfitLoss(seasonProfitLoss)
                .build();
    }

    /**
     * Build recent changes list.
     * Requirements: 11A.7
     */
    private List<ProfileDashboardResponse.RecentChange> buildRecentChanges(Long userId) {
        List<ProfileVersion> recentVersions = profileVersionRepository.findRecentByUserId(userId, 
                org.springframework.data.domain.PageRequest.of(0, 5));

        return recentVersions.stream()
                .map(v -> ProfileDashboardResponse.RecentChange.builder()
                        .changeType(v.getChangeType().name())
                        .entityType(v.getEntityType().name())
                        .description(v.getNewValue())
                        .timestamp(v.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Build livestock summary.
     * Requirements: 11A.11
     */
    private ProfileDashboardResponse.LivestockSummary buildLivestockSummary(Long userId) {
        List<Livestock> livestock = livestockRepository.findByUserIdAndIsActiveTrue(userId);

        if (livestock.isEmpty()) {
            return ProfileDashboardResponse.LivestockSummary.builder()
                    .totalLivestock(0)
                    .build();
        }

        int total = livestock.stream().mapToInt(Livestock::getQuantity).sum();

        return ProfileDashboardResponse.LivestockSummary.builder()
                .totalLivestock(total)
                .cattleCount(countByType(livestock, Livestock.LivestockType.CATTLE))
                .buffaloCount(countByType(livestock, Livestock.LivestockType.BUFFALO))
                .goatCount(countByType(livestock, Livestock.LivestockType.GOAT))
                .sheepCount(countByType(livestock, Livestock.LivestockType.SHEEP))
                .poultryCount(countByType(livestock, Livestock.LivestockType.POULTRY))
                .build();
    }

    private int countByType(List<Livestock> livestock, Livestock.LivestockType type) {
        return livestock.stream()
                .filter(l -> l.getLivestockType() == type)
                .mapToInt(Livestock::getQuantity)
                .sum();
    }

    /**
     * Build equipment summary.
     * Requirements: 11A.12
     */
    private ProfileDashboardResponse.EquipmentSummary buildEquipmentSummary(Long userId) {
        List<Equipment> equipment = equipmentRepository.findByUserIdAndIsActiveTrue(userId);

        if (equipment.isEmpty()) {
            return ProfileDashboardResponse.EquipmentSummary.builder()
                    .totalEquipment(0)
                    .build();
        }

        int total = equipment.size();
        Double totalValue = equipmentRepository.calculateTotalEquipmentValue(userId);
        int requiringMaintenance = equipmentRepository.findEquipmentRequiringMaintenance(userId, LocalDate.now()).size();

        return ProfileDashboardResponse.EquipmentSummary.builder()
                .totalEquipment(total)
                .tractors(countByEquipmentType(equipment, Equipment.EquipmentType.TRACTOR))
                .harvesters(countByEquipmentType(equipment, Equipment.EquipmentType.HARVESTER))
                .pumpSets(countByEquipmentType(equipment, Equipment.EquipmentType.PUMP_SET))
                .requiringMaintenance(requiringMaintenance)
                .totalValue(totalValue != null ? totalValue : 0.0)
                .build();
    }

    private int countByEquipmentType(List<Equipment> equipment, Equipment.EquipmentType type) {
        return (int) equipment.stream()
                .filter(e -> e.getEquipmentType() == type)
                .count();
    }
}