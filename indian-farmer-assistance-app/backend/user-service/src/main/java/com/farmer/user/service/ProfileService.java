package com.farmer.user.service;

import com.farmer.user.dto.*;
import com.farmer.user.entity.*;
import com.farmer.user.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for farmer profile management including farms, crops, livestock, and equipment.
 * Requirements: 11A.2, 11A.3, 11A.4, 11A.5, 11A.7, 11A.8, 11A.10, 11A.11, 11A.12
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final UserRepository userRepository;
    private final FarmRepository farmRepository;
    private final CropRepository cropRepository;
    private final FertilizerApplicationRepository fertilizerApplicationRepository;
    private final LivestockRepository livestockRepository;
    private final EquipmentRepository equipmentRepository;
    private final ProfileVersionRepository profileVersionRepository;

    // ==================== Farm Management ====================

    /**
     * Create a new farm for a user.
     * Requirements: 11A.2, 11A.3, 11A.10
     */
    @Transactional
    public FarmResponse createFarm(String farmerId, FarmRequest request) {
        log.info("Creating farm for farmer: {}", farmerId);

        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new UserService.UserNotFoundException("User not found: " + farmerId));

        Farm farm = Farm.builder()
                .user(user)
                .parcelNumber(request.getParcelNumber())
                .totalAreaAcres(request.getTotalAreaAcres())
                .soilType(request.getSoilType())
                .irrigationType(request.getIrrigationType())
                .agroEcologicalZone(request.getAgroEcologicalZone())
                .surveyNumber(request.getSurveyNumber())
                .gpsLatitude(request.getGpsLatitude())
                .gpsLongitude(request.getGpsLongitude())
                .village(request.getVillage())
                .district(request.getDistrict())
                .state(request.getState())
                .pinCode(request.getPinCode())
                .isActive(true)
                .build();

        farm = farmRepository.save(farm);

        // Create version history entry
        createVersionHistory(user, ProfileVersion.EntityType.FARM, farm.getId(),
                ProfileVersion.ChangeType.CREATE, "Farm created: " + farm.getParcelNumber());

        log.info("Farm created successfully: {}", farm.getId());
        return FarmResponse.fromEntity(farm);
    }

    /**
     * Get all farms for a user.
     * Requirements: 11A.10
     */
    public List<FarmResponse> getFarms(String farmerId) {
        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new UserService.UserNotFoundException("User not found: " + farmerId));

        return farmRepository.findByUserIdAndIsActiveTrue(user.getId()).stream()
                .map(FarmResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get farm by ID.
     * Requirements: 11A.10
     */
    public FarmResponse getFarm(String farmerId, Long farmId) {
        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new UserService.UserNotFoundException("User not found: " + farmerId));

        Farm farm = farmRepository.findByIdAndUserId(farmId, user.getId())
                .orElseThrow(() -> new FarmNotFoundException("Farm not found: " + farmId));

        return FarmResponse.fromEntity(farm);
    }

    /**
     * Update a farm.
     * Requirements: 11A.7, 11A.10
     */
    @Transactional
    public FarmResponse updateFarm(String farmerId, Long farmId, FarmRequest request) {
        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new UserService.UserNotFoundException("User not found: " + farmerId));

        Farm farm = farmRepository.findByIdAndUserId(farmId, user.getId())
                .orElseThrow(() -> new FarmNotFoundException("Farm not found: " + farmId));

        // Track changes for version history
        StringBuilder changes = new StringBuilder();

        if (request.getParcelNumber() != null && !request.getParcelNumber().equals(farm.getParcelNumber())) {
            changes.append("Parcel: ").append(farm.getParcelNumber()).append(" -> ").append(request.getParcelNumber()).append("; ");
            farm.setParcelNumber(request.getParcelNumber());
        }
        if (request.getTotalAreaAcres() != null && !request.getTotalAreaAcres().equals(farm.getTotalAreaAcres())) {
            changes.append("Area: ").append(farm.getTotalAreaAcres()).append(" -> ").append(request.getTotalAreaAcres()).append("; ");
            farm.setTotalAreaAcres(request.getTotalAreaAcres());
        }
        if (request.getSoilType() != null && !request.getSoilType().equals(farm.getSoilType())) {
            changes.append("Soil: ").append(farm.getSoilType()).append(" -> ").append(request.getSoilType()).append("; ");
            farm.setSoilType(request.getSoilType());
        }
        if (request.getIrrigationType() != null && request.getIrrigationType() != farm.getIrrigationType()) {
            changes.append("Irrigation: ").append(farm.getIrrigationType()).append(" -> ").append(request.getIrrigationType()).append("; ");
            farm.setIrrigationType(request.getIrrigationType());
        }
        if (request.getGpsLatitude() != null) farm.setGpsLatitude(request.getGpsLatitude());
        if (request.getGpsLongitude() != null) farm.setGpsLongitude(request.getGpsLongitude());
        if (request.getVillage() != null) farm.setVillage(request.getVillage());
        if (request.getDistrict() != null) farm.setDistrict(request.getDistrict());
        if (request.getState() != null) farm.setState(request.getState());
        if (request.getPinCode() != null) farm.setPinCode(request.getPinCode());

        farm = farmRepository.save(farm);

        // Create version history entry
        if (changes.length() > 0) {
            createVersionHistory(user, ProfileVersion.EntityType.FARM, farm.getId(),
                    ProfileVersion.ChangeType.UPDATE, changes.toString());
        }

        log.info("Farm updated successfully: {}", farm.getId());
        return FarmResponse.fromEntity(farm);
    }

    /**
     * Delete a farm (soft delete).
     * Requirements: 11A.10
     */
    @Transactional
    public void deleteFarm(String farmerId, Long farmId) {
        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new UserService.UserNotFoundException("User not found: " + farmerId));

        Farm farm = farmRepository.findByIdAndUserId(farmId, user.getId())
                .orElseThrow(() -> new FarmNotFoundException("Farm not found: " + farmId));

        farm.setIsActive(false);
        farmRepository.save(farm);

        createVersionHistory(user, ProfileVersion.EntityType.FARM, farm.getId(),
                ProfileVersion.ChangeType.DELETE, "Farm deleted: " + farm.getParcelNumber());

        log.info("Farm deleted successfully: {}", farmId);
    }

    // ==================== Crop Management ====================

    /**
     * Add a new crop to a farm.
     * Requirements: 11A.4
     */
    @Transactional
    public CropResponse addCrop(String farmerId, CropRequest request) {
        log.info("Adding crop for farmer: {}", farmerId);

        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new UserService.UserNotFoundException("User not found: " + farmerId));

        Farm farm = farmRepository.findByIdAndUserId(request.getFarmId(), user.getId())
                .orElseThrow(() -> new FarmNotFoundException("Farm not found: " + request.getFarmId()));

        Crop crop = Crop.builder()
                .farm(farm)
                .cropName(request.getCropName())
                .cropVariety(request.getCropVariety())
                .sowingDate(request.getSowingDate())
                .expectedHarvestDate(request.getExpectedHarvestDate())
                .areaAcres(request.getAreaAcres())
                .season(request.getSeason())
                .status(request.getStatus() != null ? request.getStatus() : Crop.CropStatus.SOWN)
                .seedCost(request.getSeedCost())
                .fertilizerCost(request.getFertilizerCost())
                .pesticideCost(request.getPesticideCost())
                .laborCost(request.getLaborCost())
                .otherCost(request.getOtherCost())
                .notes(request.getNotes())
                .build();

        crop.calculateTotalInputCost();
        crop = cropRepository.save(crop);

        createVersionHistory(user, ProfileVersion.EntityType.CROP, crop.getId(),
                ProfileVersion.ChangeType.CREATE, "Crop added: " + crop.getCropName());

        log.info("Crop added successfully: {}", crop.getId());
        return CropResponse.fromEntity(crop);
    }

    /**
     * Get all crops for a user.
     * Requirements: 11A.4
     */
    public List<CropResponse> getCrops(String farmerId) {
        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new UserService.UserNotFoundException("User not found: " + farmerId));

        return cropRepository.findByUserId(user.getId()).stream()
                .map(CropResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get current (active) crops for a user.
     * Requirements: 11A.4, 11A.8
     */
    public List<CropResponse> getCurrentCrops(String farmerId) {
        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new UserService.UserNotFoundException("User not found: " + farmerId));

        return cropRepository.findCurrentCropsByUserId(user.getId()).stream()
                .map(CropResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get crop by ID.
     * Requirements: 11A.4
     */
    public CropResponse getCrop(String farmerId, Long cropId) {
        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new UserService.UserNotFoundException("User not found: " + farmerId));

        Crop crop = cropRepository.findByIdAndFarmUserId(cropId, user.getId())
                .orElseThrow(() -> new CropNotFoundException("Crop not found: " + cropId));

        return CropResponse.fromEntity(crop);
    }

    /**
     * Update a crop.
     * Requirements: 11A.4, 11A.7
     */
    @Transactional
    public CropResponse updateCrop(String farmerId, Long cropId, CropRequest request) {
        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new UserService.UserNotFoundException("User not found: " + farmerId));

        Crop crop = cropRepository.findByIdAndFarmUserId(cropId, user.getId())
                .orElseThrow(() -> new CropNotFoundException("Crop not found: " + cropId));

        // Track changes
        StringBuilder changes = new StringBuilder();

        if (request.getCropName() != null && !request.getCropName().equals(crop.getCropName())) {
            changes.append("Name: ").append(crop.getCropName()).append(" -> ").append(request.getCropName()).append("; ");
            crop.setCropName(request.getCropName());
        }
        if (request.getCropVariety() != null) crop.setCropVariety(request.getCropVariety());
        if (request.getExpectedHarvestDate() != null) crop.setExpectedHarvestDate(request.getExpectedHarvestDate());
        if (request.getAreaAcres() != null) crop.setAreaAcres(request.getAreaAcres());
        if (request.getSeason() != null) crop.setSeason(request.getSeason());
        if (request.getStatus() != null) crop.setStatus(request.getStatus());
        if (request.getSeedCost() != null) crop.setSeedCost(request.getSeedCost());
        if (request.getFertilizerCost() != null) crop.setFertilizerCost(request.getFertilizerCost());
        if (request.getPesticideCost() != null) crop.setPesticideCost(request.getPesticideCost());
        if (request.getLaborCost() != null) crop.setLaborCost(request.getLaborCost());
        if (request.getOtherCost() != null) crop.setOtherCost(request.getOtherCost());
        if (request.getNotes() != null) crop.setNotes(request.getNotes());

        crop.calculateTotalInputCost();
        crop = cropRepository.save(crop);

        if (changes.length() > 0) {
            createVersionHistory(user, ProfileVersion.EntityType.CROP, crop.getId(),
                    ProfileVersion.ChangeType.UPDATE, changes.toString());
        }

        log.info("Crop updated successfully: {}", crop.getId());
        return CropResponse.fromEntity(crop);
    }

    /**
     * Record harvest data for a crop.
     * Requirements: 11A.5
     */
    @Transactional
    public CropResponse recordHarvest(String farmerId, HarvestRequest request) {
        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new UserService.UserNotFoundException("User not found: " + farmerId));

        Crop crop = cropRepository.findByIdAndFarmUserId(request.getCropId(), user.getId())
                .orElseThrow(() -> new CropNotFoundException("Crop not found: " + request.getCropId()));

        if (request.getActualHarvestDate() != null) {
            crop.setActualHarvestDate(request.getActualHarvestDate());
        }
        if (request.getTotalYieldQuintals() != null) {
            crop.setTotalYieldQuintals(request.getTotalYieldQuintals());
        }
        if (request.getQualityGrade() != null) {
            crop.setQualityGrade(request.getQualityGrade());
        }
        if (request.getSellingPricePerQuintal() != null) {
            crop.setSellingPricePerQuintal(request.getSellingPricePerQuintal());
        }
        if (request.getMandiName() != null) {
            crop.setMandiName(request.getMandiName());
        }

        crop.setStatus(Crop.CropStatus.HARVESTED);
        crop.calculateTotalRevenue();

        crop = cropRepository.save(crop);

        createVersionHistory(user, ProfileVersion.EntityType.HARVEST_RECORD, crop.getId(),
                ProfileVersion.ChangeType.UPDATE, "Harvest recorded: " + crop.getTotalYieldQuintals() + " quintals");

        log.info("Harvest recorded for crop: {}", crop.getId());
        return CropResponse.fromEntity(crop);
    }

    /**
     * Delete a crop (soft delete).
     * Requirements: 11A.4
     */
    @Transactional
    public void deleteCrop(String farmerId, Long cropId) {
        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new UserService.UserNotFoundException("User not found: " + farmerId));

        Crop crop = cropRepository.findByIdAndFarmUserId(cropId, user.getId())
                .orElseThrow(() -> new CropNotFoundException("Crop not found: " + cropId));

        crop.setStatus(Crop.CropStatus.FAILED);
        cropRepository.save(crop);

        createVersionHistory(user, ProfileVersion.EntityType.CROP, crop.getId(),
                ProfileVersion.ChangeType.DELETE, "Crop deleted: " + crop.getCropName());

        log.info("Crop deleted successfully: {}", cropId);
    }

    // ==================== Fertilizer Management ====================

    /**
     * Add fertilizer application to a crop.
     * Requirements: 11A.4
     */
    @Transactional
    public FertilizerApplicationResponse addFertilizerApplication(String farmerId, Long cropId, FertilizerApplication application) {
        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new UserService.UserNotFoundException("User not found: " + farmerId));

        Crop crop = cropRepository.findByIdAndFarmUserId(cropId, user.getId())
                .orElseThrow(() -> new CropNotFoundException("Crop not found: " + cropId));

        application.setCrop(crop);
        application = fertilizerApplicationRepository.save(application);

        log.info("Fertilizer application added: {}", application.getId());
        return FertilizerApplicationResponse.fromEntity(application);
    }

    /**
     * Get fertilizer applications for a crop.
     * Requirements: 11A.4
     */
    public List<FertilizerApplicationResponse> getFertilizerApplications(String farmerId, Long cropId) {
        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new UserService.UserNotFoundException("User not found: " + farmerId));

        Crop crop = cropRepository.findByIdAndFarmUserId(cropId, user.getId())
                .orElseThrow(() -> new CropNotFoundException("Crop not found: " + cropId));

        return fertilizerApplicationRepository.findByCropId(crop.getId()).stream()
                .map(FertilizerApplicationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ==================== Livestock Management ====================

    /**
     * Add livestock for a user.
     * Requirements: 11A.11
     */
    @Transactional
    public LivestockResponse addLivestock(String farmerId, LivestockRequest request) {
        log.info("Adding livestock for farmer: {}", farmerId);

        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new UserService.UserNotFoundException("User not found: " + farmerId));

        Farm farm = null;
        if (request.getFarmId() != null) {
            farm = farmRepository.findByIdAndUserId(request.getFarmId(), user.getId())
                    .orElseThrow(() -> new FarmNotFoundException("Farm not found: " + request.getFarmId()));
        }

        Livestock livestock = Livestock.builder()
                .user(user)
                .farm(farm)
                .livestockType(request.getLivestockType())
                .breed(request.getBreed())
                .quantity(request.getQuantity())
                .purpose(request.getPurpose())
                .acquisitionDate(request.getAcquisitionDate())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .tagNumber(request.getTagNumber())
                .healthStatus(request.getHealthStatus())
                .vaccinationStatus(request.getVaccinationStatus())
                .notes(request.getNotes())
                .isActive(true)
                .build();

        livestock = livestockRepository.save(livestock);

        createVersionHistory(user, ProfileVersion.EntityType.LIVESTOCK, livestock.getId(),
                ProfileVersion.ChangeType.CREATE, "Livestock added: " + livestock.getLivestockType());

        log.info("Livestock added successfully: {}", livestock.getId());
        return LivestockResponse.fromEntity(livestock);
    }

    /**
     * Get all livestock for a user.
     * Requirements: 11A.11
     */
    public List<LivestockResponse> getLivestock(String farmerId) {
        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new UserService.UserNotFoundException("User not found: " + farmerId));

        return livestockRepository.findByUserIdAndIsActiveTrue(user.getId()).stream()
                .map(LivestockResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Update livestock.
     * Requirements: 11A.11
     */
    @Transactional
    public LivestockResponse updateLivestock(String farmerId, Long livestockId, LivestockRequest request) {
        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new UserService.UserNotFoundException("User not found: " + farmerId));

        Livestock livestock = livestockRepository.findByIdAndUserId(livestockId, user.getId())
                .orElseThrow(() -> new LivestockNotFoundException("Livestock not found: " + livestockId));

        if (request.getLivestockType() != null) livestock.setLivestockType(request.getLivestockType());
        if (request.getBreed() != null) livestock.setBreed(request.getBreed());
        if (request.getQuantity() != null) livestock.setQuantity(request.getQuantity());
        if (request.getPurpose() != null) livestock.setPurpose(request.getPurpose());
        if (request.getHealthStatus() != null) livestock.setHealthStatus(request.getHealthStatus());
        if (request.getVaccinationStatus() != null) livestock.setVaccinationStatus(request.getVaccinationStatus());
        if (request.getNotes() != null) livestock.setNotes(request.getNotes());

        livestock = livestockRepository.save(livestock);

        log.info("Livestock updated successfully: {}", livestock.getId());
        return LivestockResponse.fromEntity(livestock);
    }

    /**
     * Delete livestock (soft delete).
     * Requirements: 11A.11
     */
    @Transactional
    public void deleteLivestock(String farmerId, Long livestockId) {
        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new UserService.UserNotFoundException("User not found: " + farmerId));

        Livestock livestock = livestockRepository.findByIdAndUserId(livestockId, user.getId())
                .orElseThrow(() -> new LivestockNotFoundException("Livestock not found: " + livestockId));

        livestock.setIsActive(false);
        livestockRepository.save(livestock);

        log.info("Livestock deleted successfully: {}", livestockId);
    }

    // ==================== Equipment Management ====================

    /**
     * Add equipment for a user.
     * Requirements: 11A.12
     */
    @Transactional
    public EquipmentResponse addEquipment(String farmerId, EquipmentRequest request) {
        log.info("Adding equipment for farmer: {}", farmerId);

        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new UserService.UserNotFoundException("User not found: " + farmerId));

        Equipment equipment = Equipment.builder()
                .user(user)
                .equipmentType(request.getEquipmentType())
                .equipmentName(request.getEquipmentName())
                .manufacturer(request.getManufacturer())
                .model(request.getModel())
                .serialNumber(request.getSerialNumber())
                .purchaseDate(request.getPurchaseDate())
                .purchaseCost(request.getPurchaseCost())
                .ownershipType(request.getOwnershipType())
                .lastMaintenanceDate(request.getLastMaintenanceDate())
                .nextMaintenanceDate(request.getNextMaintenanceDate())
                .currentValue(request.getCurrentValue())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .notes(request.getNotes())
                .isActive(true)
                .build();

        equipment = equipmentRepository.save(equipment);

        createVersionHistory(user, ProfileVersion.EntityType.EQUIPMENT, equipment.getId(),
                ProfileVersion.ChangeType.CREATE, "Equipment added: " + equipment.getEquipmentType());

        log.info("Equipment added successfully: {}", equipment.getId());
        return EquipmentResponse.fromEntity(equipment);
    }

    /**
     * Get all equipment for a user.
     * Requirements: 11A.12
     */
    public List<EquipmentResponse> getEquipment(String farmerId) {
        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new UserService.UserNotFoundException("User not found: " + farmerId));

        return equipmentRepository.findByUserIdAndIsActiveTrue(user.getId()).stream()
                .map(EquipmentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Update equipment.
     * Requirements: 11A.12
     */
    @Transactional
    public EquipmentResponse updateEquipment(String farmerId, Long equipmentId, EquipmentRequest request) {
        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new UserService.UserNotFoundException("User not found: " + farmerId));

        Equipment equipment = equipmentRepository.findByIdAndUserId(equipmentId, user.getId())
                .orElseThrow(() -> new EquipmentNotFoundException("Equipment not found: " + equipmentId));

        if (request.getEquipmentType() != null) equipment.setEquipmentType(request.getEquipmentType());
        if (request.getEquipmentName() != null) equipment.setEquipmentName(request.getEquipmentName());
        if (request.getManufacturer() != null) equipment.setManufacturer(request.getManufacturer());
        if (request.getModel() != null) equipment.setModel(request.getModel());
        if (request.getLastMaintenanceDate() != null) equipment.setLastMaintenanceDate(request.getLastMaintenanceDate());
        if (request.getNextMaintenanceDate() != null) equipment.setNextMaintenanceDate(request.getNextMaintenanceDate());
        if (request.getCurrentValue() != null) equipment.setCurrentValue(request.getCurrentValue());
        if (request.getStatus() != null) equipment.setStatus(request.getStatus());
        if (request.getNotes() != null) equipment.setNotes(request.getNotes());

        equipment = equipmentRepository.save(equipment);

        log.info("Equipment updated successfully: {}", equipment.getId());
        return EquipmentResponse.fromEntity(equipment);
    }

    /**
     * Delete equipment (soft delete).
     * Requirements: 11A.12
     */
    @Transactional
    public void deleteEquipment(String farmerId, Long equipmentId) {
        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new UserService.UserNotFoundException("User not found: " + farmerId));

        Equipment equipment = equipmentRepository.findByIdAndUserId(equipmentId, user.getId())
                .orElseThrow(() -> new EquipmentNotFoundException("Equipment not found: " + equipmentId));

        equipment.setIsActive(false);
        equipmentRepository.save(equipment);

        log.info("Equipment deleted successfully: {}", equipmentId);
    }

    // ==================== Version History ====================

    /**
     * Get version history for a user.
     * Requirements: 11A.7
     */
    public List<ProfileVersion> getVersionHistory(String farmerId) {
        User user = userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new UserService.UserNotFoundException("User not found: " + farmerId));

        return profileVersionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    /**
     * Create version history entry.
     */
    private void createVersionHistory(User user, ProfileVersion.EntityType entityType, Long entityId,
                                      ProfileVersion.ChangeType changeType, String description) {
        Long latestVersion = profileVersionRepository.getLatestVersionNumber(user.getId());

        ProfileVersion version = ProfileVersion.builder()
                .userId(user.getId())
                .farmerId(user.getFarmerId())
                .entityType(entityType)
                .entityId(entityId)
                .changeType(changeType)
                .newValue(description)
                .changedBy(user.getFarmerId())
                .versionNumber(latestVersion + 1)
                .build();

        profileVersionRepository.save(version);
    }

    // ==================== Custom Exceptions ====================

    public static class FarmNotFoundException extends RuntimeException {
        public FarmNotFoundException(String message) {
            super(message);
        }
    }

    public static class CropNotFoundException extends RuntimeException {
        public CropNotFoundException(String message) {
            super(message);
        }
    }

    public static class LivestockNotFoundException extends RuntimeException {
        public LivestockNotFoundException(String message) {
            super(message);
        }
    }

    public static class EquipmentNotFoundException extends RuntimeException {
        public EquipmentNotFoundException(String message) {
            super(message);
        }
    }
}