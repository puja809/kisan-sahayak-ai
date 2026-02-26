package com.farmer.user;

import com.farmer.user.dto.*;
import com.farmer.user.entity.*;
import com.farmer.user.repository.*;
import com.farmer.user.service.ProfileDashboardService;
import com.farmer.user.service.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Profile Management functionality.
 * 
 * Validates: Requirements 11A.1, 11A.7, 11A.8
 * 
 * Test Cases:
 * 1. Test profile creation with all required fields
 * 2. Test profile updates and version tracking
 * 3. Test crop record CRUD operations
 * 4. Test dashboard data aggregation
 */
@ExtendWith(MockitoExtension.class)
class ProfileManagementTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private CropRepository cropRepository;

    @Mock
    private FertilizerApplicationRepository fertilizerApplicationRepository;

    @Mock
    private LivestockRepository livestockRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private ProfileVersionRepository profileVersionRepository;

    @InjectMocks
    private ProfileService profileService;

    @InjectMocks
    private ProfileDashboardService profileDashboardService;

    private User testUser;
    private Farm testFarm;

    @BeforeEach
    void setUp() {
        // Create test user with all required fields per Requirement 11A.1
        testUser = User.builder()
                .id(1L)
                .farmerId("FARMER-TEST-001")
                .aadhaarHash("aadhaarhash123456")
                .name("Test Farmer")
                .phone("9876543210")
                .email("test@farmer.com")
                .preferredLanguage("en")
                .state("Maharashtra")
                .district("Pune")
                .village("Test Village")
                .pinCode("411001")
                .gpsLatitude(18.5204)
                .gpsLongitude(73.8567)
                .role(User.Role.FARMER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        // Create test farm
        testFarm = Farm.builder()
                .id(1L)
                .user(testUser)
                .parcelNumber("PARCEL-001")
                .totalAreaAcres(5.0)
                .soilType("Red Loam")
                .irrigationType(Farm.IrrigationType.CANAL)
                .agroEcologicalZone("Western Plateau and Hills")
                .surveyNumber("SURVEY-123")
                .gpsLatitude(18.5204)
                .gpsLongitude(73.8567)
                .village("Test Village")
                .district("Pune")
                .state("Maharashtra")
                .pinCode("411001")
                .isActive(true)
                .build();
    }

    @Nested
    @DisplayName("Requirement 11A.1: Profile Creation with Required Fields")
    class ProfileCreationTest {

        @Test
        @DisplayName("Should create farm with all required fields")
        void shouldCreateFarmWithAllRequiredFields() {
            // Given
            FarmRequest request = FarmRequest.builder()
                    .parcelNumber("PARCEL-NEW-001")
                    .totalAreaAcres(10.0)
                    .soilType("Black Cotton")
                    .irrigationType(Farm.IrrigationType.BOREWELL)
                    .agroEcologicalZone("Central Plateau and Hills")
                    .surveyNumber("SURVEY-456")
                    .gpsLatitude(18.5204)
                    .gpsLongitude(73.8567)
                    .village("New Village")
                    .district("Pune")
                    .state("Maharashtra")
                    .pinCode("411002")
                    .build();

            when(userRepository.findByFarmerId(testUser.getFarmerId())).thenReturn(Optional.of(testUser));
            when(farmRepository.save(any(Farm.class))).thenAnswer(invocation -> {
                Farm farm = invocation.getArgument(0);
                farm.setId(2L);
                return farm;
            });
            when(profileVersionRepository.getLatestVersionNumber(testUser.getId())).thenReturn(0L);
            when(profileVersionRepository.save(any(ProfileVersion.class))).thenAnswer(invocation -> {
                ProfileVersion version = invocation.getArgument(0);
                version.setId(1L);
                return version;
            });

            // When
            FarmResponse response = profileService.createFarm(testUser.getFarmerId(), request);

            // Then
            assertNotNull(response);
            assertEquals("PARCEL-NEW-001", response.getParcelNumber());
            assertEquals(10.0, response.getTotalAreaAcres());
            assertEquals("Black Cotton", response.getSoilType());
            assertEquals(Farm.IrrigationType.BOREWELL.name(), response.getIrrigationType());
            assertEquals("Central Plateau and Hills", response.getAgroEcologicalZone());
            assertEquals("New Village", response.getVillage());
            assertEquals("Pune", response.getDistrict());
            assertEquals("Maharashtra", response.getState());
            assertEquals("411002", response.getPinCode());
            
            verify(farmRepository).save(any(Farm.class));
            verify(profileVersionRepository).save(any(ProfileVersion.class));
        }

        @Test
        @DisplayName("Should add crop with all required fields per Requirement 11A.4")
        void shouldAddCropWithAllRequiredFields() {
            // Given
            CropRequest request = CropRequest.builder()
                    .farmId(testFarm.getId())
                    .cropName("Paddy")
                    .cropVariety("IR-64")
                    .sowingDate(LocalDate.of(2024, 6, 1))
                    .expectedHarvestDate(LocalDate.of(2024, 10, 15))
                    .areaAcres(2.5)
                    .season(Crop.Season.KHARIF)
                    .status(Crop.CropStatus.SOWN)
                    .seedCost(Double.valueOf("2500.00"))
                    .fertilizerCost(Double.valueOf("1500.00"))
                    .pesticideCost(Double.valueOf("800.00"))
                    .laborCost(Double.valueOf("2000.00"))
                    .otherCost(Double.valueOf("500.00"))
                    .notes("Test crop")
                    .build();

            when(userRepository.findByFarmerId(testUser.getFarmerId())).thenReturn(Optional.of(testUser));
            when(farmRepository.findByIdAndUserId(testFarm.getId(), testUser.getId())).thenReturn(Optional.of(testFarm));
            when(cropRepository.save(any(Crop.class))).thenAnswer(invocation -> {
                Crop crop = invocation.getArgument(0);
                crop.setId(1L);
                return crop;
            });
            when(profileVersionRepository.getLatestVersionNumber(testUser.getId())).thenReturn(0L);
            when(profileVersionRepository.save(any(ProfileVersion.class))).thenAnswer(invocation -> {
                ProfileVersion version = invocation.getArgument(0);
                version.setId(1L);
                return version;
            });

            // When
            CropResponse response = profileService.addCrop(testUser.getFarmerId(), request);

            // Then
            assertNotNull(response);
            assertEquals("Paddy", response.getCropName());
            assertEquals("IR-64", response.getCropVariety());
            assertEquals(LocalDate.of(2024, 6, 1), response.getSowingDate());
            assertEquals(LocalDate.of(2024, 10, 15), response.getExpectedHarvestDate());
            assertEquals(2.5, response.getAreaAcres());
            assertEquals(Crop.Season.KHARIF.name(), response.getSeason());
            assertEquals(Crop.CropStatus.SOWN.name(), response.getStatus());
            assertEquals(0, Double.valueOf("2500.00").compareTo(response.getSeedCost()));
            assertEquals(0, Double.valueOf("1500.00").compareTo(response.getFertilizerCost()));
            assertEquals(0, Double.valueOf("800.00").compareTo(response.getPesticideCost()));
            assertEquals(0, Double.valueOf("2000.00").compareTo(response.getLaborCost()));
            assertEquals(0, Double.valueOf("500.00").compareTo(response.getOtherCost()));
            
            verify(cropRepository).save(any(Crop.class));
            verify(profileVersionRepository).save(any(ProfileVersion.class));
        }

        @Test
        @DisplayName("Should record harvest data per Requirement 11A.5")
        void shouldRecordHarvestData() {
            // Given
            Crop crop = Crop.builder()
                    .id(1L)
                    .farm(testFarm)
                    .cropName("Paddy")
                    .cropVariety("IR-64")
                    .sowingDate(LocalDate.of(2024, 6, 1))
                    .expectedHarvestDate(LocalDate.of(2024, 10, 15))
                    .areaAcres(2.5)
                    .season(Crop.Season.KHARIF)
                    .status(Crop.CropStatus.GROWING)
                    .build();

            HarvestRequest request = HarvestRequest.builder()
                    .cropId(crop.getId())
                    .actualHarvestDate(LocalDate.of(2024, 10, 20))
                    .totalYieldQuintals(Double.valueOf("12.5"))
                    .qualityGrade("A")
                    .sellingPricePerQuintal(Double.valueOf("2200.00"))
                    .mandiName("Pune Mandi")
                    .build();

            when(userRepository.findByFarmerId(testUser.getFarmerId())).thenReturn(Optional.of(testUser));
            when(cropRepository.findByIdAndFarmUserId(crop.getId(), testUser.getId())).thenReturn(Optional.of(crop));
            when(cropRepository.save(any(Crop.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(profileVersionRepository.getLatestVersionNumber(testUser.getId())).thenReturn(0L);
            when(profileVersionRepository.save(any(ProfileVersion.class))).thenAnswer(invocation -> {
                ProfileVersion version = invocation.getArgument(0);
                version.setId(1L);
                return version;
            });

            // When
            CropResponse response = profileService.recordHarvest(testUser.getFarmerId(), request);

            // Then
            assertNotNull(response);
            assertEquals(Crop.CropStatus.HARVESTED.name(), response.getStatus());
            assertEquals(LocalDate.of(2024, 10, 20), response.getActualHarvestDate());
            assertEquals(0, Double.valueOf("12.5").compareTo(response.getTotalYieldQuintals()));
            assertEquals("A", response.getQualityGrade());
            assertEquals(0, Double.valueOf("2200.00").compareTo(response.getSellingPricePerQuintal()));
            assertEquals("Pune Mandi", response.getMandiName());
            
            verify(cropRepository).save(any(Crop.class));
            verify(profileVersionRepository).save(any(ProfileVersion.class));
        }
    }

    @Nested
    @DisplayName("Requirement 11A.7: Profile Updates and Version Tracking")
    class ProfileUpdatesAndVersionTrackingTest {

        @Test
        @DisplayName("Should update farm and create version history entry")
        void shouldUpdateFarmAndCreateVersionHistory() {
            // Given
            FarmRequest request = FarmRequest.builder()
                    .totalAreaAcres(7.5)
                    .soilType("Laterite")
                    .irrigationType(Farm.IrrigationType.DRIP)
                    .build();

            when(userRepository.findByFarmerId(testUser.getFarmerId())).thenReturn(Optional.of(testUser));
            when(farmRepository.findByIdAndUserId(testFarm.getId(), testUser.getId())).thenReturn(Optional.of(testFarm));
            when(farmRepository.save(any(Farm.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(profileVersionRepository.getLatestVersionNumber(testUser.getId())).thenReturn(5L);
            when(profileVersionRepository.save(any(ProfileVersion.class))).thenAnswer(invocation -> {
                ProfileVersion version = invocation.getArgument(0);
                version.setId(1L);
                return version;
            });

            // When
            FarmResponse response = profileService.updateFarm(testUser.getFarmerId(), testFarm.getId(), request);

            // Then
            assertNotNull(response);
            assertEquals(7.5, response.getTotalAreaAcres());
            assertEquals("Laterite", response.getSoilType());
            assertEquals(Farm.IrrigationType.DRIP.name(), response.getIrrigationType());
            
            verify(farmRepository).save(any(Farm.class));
            verify(profileVersionRepository).save(argThat(version -> 
                version.getVersionNumber() == 6L &&
                version.getChangeType() == ProfileVersion.ChangeType.UPDATE &&
                version.getEntityType() == ProfileVersion.EntityType.FARM
            ));
        }

        @Test
        @DisplayName("Should update crop and create version history entry")
        void shouldUpdateCropAndCreateVersionHistory() {
            // Given
            Crop crop = Crop.builder()
                    .id(1L)
                    .farm(testFarm)
                    .cropName("Paddy")
                    .cropVariety("IR-64")
                    .sowingDate(LocalDate.of(2024, 6, 1))
                    .expectedHarvestDate(LocalDate.of(2024, 10, 15))
                    .areaAcres(2.5)
                    .season(Crop.Season.KHARIF)
                    .status(Crop.CropStatus.SOWN)
                    .build();

            CropRequest request = CropRequest.builder()
                    .cropName("Basmati Rice")
                    .cropVariety("Basmati-1509")
                    .areaAcres(3.0)
                    .status(Crop.CropStatus.GROWING)
                    .build();

            when(userRepository.findByFarmerId(testUser.getFarmerId())).thenReturn(Optional.of(testUser));
            when(cropRepository.findByIdAndFarmUserId(crop.getId(), testUser.getId())).thenReturn(Optional.of(crop));
            when(cropRepository.save(any(Crop.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(profileVersionRepository.getLatestVersionNumber(testUser.getId())).thenReturn(3L);
            when(profileVersionRepository.save(any(ProfileVersion.class))).thenAnswer(invocation -> {
                ProfileVersion version = invocation.getArgument(0);
                version.setId(1L);
                return version;
            });

            // When
            CropResponse response = profileService.updateCrop(testUser.getFarmerId(), crop.getId(), request);

            // Then
            assertNotNull(response);
            assertEquals("Basmati Rice", response.getCropName());
            assertEquals("Basmati-1509", response.getCropVariety());
            assertEquals(3.0, response.getAreaAcres());
            assertEquals(Crop.CropStatus.GROWING.name(), response.getStatus());
            
            verify(cropRepository).save(any(Crop.class));
            verify(profileVersionRepository).save(argThat(version -> 
                version.getVersionNumber() == 4L &&
                version.getChangeType() == ProfileVersion.ChangeType.UPDATE &&
                version.getEntityType() == ProfileVersion.EntityType.CROP
            ));
        }

        @Test
        @DisplayName("Should get version history for a user")
        void shouldGetVersionHistory() {
            // Given
            List<ProfileVersion> versions = List.of(
                ProfileVersion.builder()
                        .id(1L)
                        .userId(testUser.getId())
                        .farmerId(testUser.getFarmerId())
                        .entityType(ProfileVersion.EntityType.CROP)
                        .entityId(1L)
                        .changeType(ProfileVersion.ChangeType.CREATE)
                        .versionNumber(1L)
                        .createdAt(LocalDateTime.now())
                        .build(),
                ProfileVersion.builder()
                        .id(2L)
                        .userId(testUser.getId())
                        .farmerId(testUser.getFarmerId())
                        .entityType(ProfileVersion.EntityType.CROP)
                        .entityId(1L)
                        .changeType(ProfileVersion.ChangeType.UPDATE)
                        .versionNumber(2L)
                        .createdAt(LocalDateTime.now())
                        .build()
            );

            when(userRepository.findByFarmerId(testUser.getFarmerId())).thenReturn(Optional.of(testUser));
            when(profileVersionRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId())).thenReturn(versions);

            // When
            List<ProfileVersion> history = profileService.getVersionHistory(testUser.getFarmerId());

            // Then
            assertNotNull(history);
            assertEquals(2, history.size());
            // The list is ordered by createdAt descending, so CREATE (first) comes before UPDATE (second)
            assertEquals(ProfileVersion.ChangeType.CREATE, history.get(0).getChangeType());
            assertEquals(ProfileVersion.ChangeType.UPDATE, history.get(1).getChangeType());
        }

        @Test
        @DisplayName("Should soft delete farm and create version history")
        void shouldSoftDeleteFarmAndCreateVersionHistory() {
            // Given
            when(userRepository.findByFarmerId(testUser.getFarmerId())).thenReturn(Optional.of(testUser));
            when(farmRepository.findByIdAndUserId(testFarm.getId(), testUser.getId())).thenReturn(Optional.of(testFarm));
            when(farmRepository.save(any(Farm.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(profileVersionRepository.getLatestVersionNumber(testUser.getId())).thenReturn(2L);
            when(profileVersionRepository.save(any(ProfileVersion.class))).thenAnswer(invocation -> {
                ProfileVersion version = invocation.getArgument(0);
                version.setId(1L);
                return version;
            });

            // When
            profileService.deleteFarm(testUser.getFarmerId(), testFarm.getId());

            // Then
            verify(farmRepository).save(argThat(farm -> !farm.getIsActive()));
            verify(profileVersionRepository).save(argThat(version -> 
                version.getChangeType() == ProfileVersion.ChangeType.DELETE &&
                version.getEntityType() == ProfileVersion.EntityType.FARM
            ));
        }
    }

    @Nested
    @DisplayName("Requirement 11A.4: Crop Record CRUD Operations")
    class CropRecordCrudOperationsTest {

        @Test
        @DisplayName("Should get all crops for a user")
        void shouldGetAllCropsForUser() {
            // Given
            List<Crop> crops = List.of(
                Crop.builder()
                        .id(1L)
                        .farm(testFarm)
                        .cropName("Paddy")
                        .cropVariety("IR-64")
                        .sowingDate(LocalDate.of(2024, 6, 1))
                        .areaAcres(2.5)
                        .season(Crop.Season.KHARIF)
                        .status(Crop.CropStatus.SOWN)
                        .build(),
                Crop.builder()
                        .id(2L)
                        .farm(testFarm)
                        .cropName("Wheat")
                        .cropVariety("HD-2967")
                        .sowingDate(LocalDate.of(2023, 11, 1))
                        .areaAcres(3.0)
                        .season(Crop.Season.RABI)
                        .status(Crop.CropStatus.HARVESTED)
                        .build()
            );

            when(userRepository.findByFarmerId(testUser.getFarmerId())).thenReturn(Optional.of(testUser));
            when(cropRepository.findByUserId(testUser.getId())).thenReturn(crops);

            // When
            List<CropResponse> response = profileService.getCrops(testUser.getFarmerId());

            // Then
            assertNotNull(response);
            assertEquals(2, response.size());
            assertEquals("Paddy", response.get(0).getCropName());
            assertEquals("Wheat", response.get(1).getCropName());
        }

        @Test
        @DisplayName("Should get current (active) crops for a user")
        void shouldGetCurrentCropsForUser() {
            // Given
            List<Crop> currentCrops = List.of(
                Crop.builder()
                        .id(1L)
                        .farm(testFarm)
                        .cropName("Paddy")
                        .cropVariety("IR-64")
                        .sowingDate(LocalDate.of(2024, 6, 1))
                        .areaAcres(2.5)
                        .season(Crop.Season.KHARIF)
                        .status(Crop.CropStatus.GROWING)
                        .build()
            );

            when(userRepository.findByFarmerId(testUser.getFarmerId())).thenReturn(Optional.of(testUser));
            when(cropRepository.findCurrentCropsByUserId(testUser.getId())).thenReturn(currentCrops);

            // When
            List<CropResponse> response = profileService.getCurrentCrops(testUser.getFarmerId());

            // Then
            assertNotNull(response);
            assertEquals(1, response.size());
            assertEquals("Paddy", response.get(0).getCropName());
            assertEquals(Crop.CropStatus.GROWING.name(), response.get(0).getStatus());
        }

        @Test
        @DisplayName("Should get crop by ID")
        void shouldGetCropById() {
            // Given
            Crop crop = Crop.builder()
                    .id(1L)
                    .farm(testFarm)
                    .cropName("Paddy")
                    .cropVariety("IR-64")
                    .sowingDate(LocalDate.of(2024, 6, 1))
                    .areaAcres(2.5)
                    .season(Crop.Season.KHARIF)
                    .status(Crop.CropStatus.SOWN)
                    .build();

            when(userRepository.findByFarmerId(testUser.getFarmerId())).thenReturn(Optional.of(testUser));
            when(cropRepository.findByIdAndFarmUserId(crop.getId(), testUser.getId())).thenReturn(Optional.of(crop));

            // When
            CropResponse response = profileService.getCrop(testUser.getFarmerId(), crop.getId());

            // Then
            assertNotNull(response);
            assertEquals("Paddy", response.getCropName());
            assertEquals("IR-64", response.getCropVariety());
        }

        @Test
        @DisplayName("Should soft delete crop and create version history")
        void shouldSoftDeleteCropAndCreateVersionHistory() {
            // Given
            Crop crop = Crop.builder()
                    .id(1L)
                    .farm(testFarm)
                    .cropName("Paddy")
                    .sowingDate(LocalDate.of(2024, 6, 1))
                    .areaAcres(2.5)
                    .status(Crop.CropStatus.SOWN)
                    .build();

            when(userRepository.findByFarmerId(testUser.getFarmerId())).thenReturn(Optional.of(testUser));
            when(cropRepository.findByIdAndFarmUserId(crop.getId(), testUser.getId())).thenReturn(Optional.of(crop));
            when(cropRepository.save(any(Crop.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(profileVersionRepository.getLatestVersionNumber(testUser.getId())).thenReturn(1L);
            when(profileVersionRepository.save(any(ProfileVersion.class))).thenAnswer(invocation -> {
                ProfileVersion version = invocation.getArgument(0);
                version.setId(1L);
                return version;
            });

            // When
            profileService.deleteCrop(testUser.getFarmerId(), crop.getId());

            // Then
            verify(cropRepository).save(argThat(c -> c.getStatus() == Crop.CropStatus.FAILED));
            verify(profileVersionRepository).save(argThat(version -> 
                version.getChangeType() == ProfileVersion.ChangeType.DELETE &&
                version.getEntityType() == ProfileVersion.EntityType.CROP
            ));
        }
    }

    @Nested
    @DisplayName("Requirement 11A.8: Dashboard Data Aggregation")
    class DashboardDataAggregationTest {

        @Test
        @DisplayName("Should get complete dashboard with all components")
        void shouldGetCompleteDashboardWithAllComponents() {
            // Given
            List<Farm> farms = List.of(testFarm);
            List<Crop> currentCrops = List.of(
                Crop.builder()
                        .id(1L)
                        .farm(testFarm)
                        .cropName("Paddy")
                        .cropVariety("IR-64")
                        .sowingDate(LocalDate.now().minusDays(30))
                        .expectedHarvestDate(LocalDate.now().plusDays(60))
                        .areaAcres(2.5)
                        .season(Crop.Season.KHARIF)
                        .status(Crop.CropStatus.GROWING)
                        .build()
            );
            List<ProfileVersion> recentVersions = List.of(
                ProfileVersion.builder()
                        .id(1L)
                        .userId(testUser.getId())
                        .farmerId(testUser.getFarmerId())
                        .entityType(ProfileVersion.EntityType.CROP)
                        .changeType(ProfileVersion.ChangeType.CREATE)
                        .newValue("Crop added: Paddy")
                        .createdAt(LocalDateTime.now())
                        .build()
            );

            when(userRepository.findByFarmerId(testUser.getFarmerId())).thenReturn(Optional.of(testUser));
            when(farmRepository.findByUserIdAndIsActiveTrue(testUser.getId())).thenReturn(farms);
            when(cropRepository.findCurrentCropsByUserId(testUser.getId())).thenReturn(currentCrops);
            when(cropRepository.findUpcomingHarvests(eq(testUser.getId()), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(currentCrops);
            when(cropRepository.findByFarmUserIdAndStatus(testUser.getId(), Crop.CropStatus.GROWING))
                    .thenReturn(currentCrops);
            when(fertilizerApplicationRepository.countByCropId(any())).thenReturn(0L);
            when(cropRepository.calculateTotalInputCost(eq(testUser.getId()), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(5000.0);
            when(cropRepository.calculateTotalRevenue(eq(testUser.getId()), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(15000.0);
            when(profileVersionRepository.findRecentByUserId(eq(testUser.getId()), any(PageRequest.class)))
                    .thenReturn(recentVersions);
            when(livestockRepository.findByUserIdAndIsActiveTrue(testUser.getId())).thenReturn(List.of());
            when(equipmentRepository.findByUserIdAndIsActiveTrue(testUser.getId())).thenReturn(List.of());

            // When
            ProfileDashboardResponse response = profileDashboardService.getDashboard(testUser.getFarmerId());

            // Then
            assertNotNull(response);
            assertNotNull(response.getUser());
            assertNotNull(response.getFarmSummary());
            assertNotNull(response.getCurrentCrops());
            assertNotNull(response.getUpcomingActivities());
            assertNotNull(response.getFinancialSummary());
            assertNotNull(response.getRecentChanges());
            
            // Verify user info
            assertEquals(testUser.getName(), response.getUser().getName());
            assertEquals(testUser.getPhone(), response.getUser().getPhone());
            
            // Verify farm summary
            assertEquals(1, response.getFarmSummary().getTotalParcels());
            assertEquals(5.0, response.getFarmSummary().getTotalAreaAcres());
            assertEquals("Red Loam", response.getFarmSummary().getPrimarySoilType());
            
            // Verify current crops
            assertEquals(1, response.getCurrentCrops().size());
            assertEquals("Paddy", response.getCurrentCrops().get(0).getCropName());
            
            // Verify financial summary
            assertEquals(0, Double.valueOf("5000.00").compareTo(response.getFinancialSummary().getTotalInputCosts()));
            assertEquals(0, Double.valueOf("15000.00").compareTo(response.getFinancialSummary().getTotalRevenue()));
            assertEquals(0, Double.valueOf("10000.00").compareTo(response.getFinancialSummary().getProfitLoss()));
        }

        @Test
        @DisplayName("Should calculate financial summary correctly")
        void shouldCalculateFinancialSummaryCorrectly() {
            // Given
            when(userRepository.findByFarmerId(testUser.getFarmerId())).thenReturn(Optional.of(testUser));
            when(farmRepository.findByUserIdAndIsActiveTrue(testUser.getId())).thenReturn(List.of());
            when(cropRepository.findCurrentCropsByUserId(testUser.getId())).thenReturn(List.of());
            when(cropRepository.findUpcomingHarvests(eq(testUser.getId()), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(List.of());
            when(cropRepository.findByFarmUserIdAndStatus(testUser.getId(), Crop.CropStatus.GROWING))
                    .thenReturn(List.of());
            when(cropRepository.calculateTotalInputCost(eq(testUser.getId()), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(10000.0);
            when(cropRepository.calculateTotalRevenue(eq(testUser.getId()), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(25000.0);
            when(profileVersionRepository.findRecentByUserId(eq(testUser.getId()), any(PageRequest.class)))
                    .thenReturn(List.of());
            when(livestockRepository.findByUserIdAndIsActiveTrue(testUser.getId())).thenReturn(List.of());
            when(equipmentRepository.findByUserIdAndIsActiveTrue(testUser.getId())).thenReturn(List.of());

            // When
            ProfileDashboardResponse response = profileDashboardService.getDashboard(testUser.getFarmerId());

            // Then
            assertNotNull(response.getFinancialSummary());
            assertEquals(0, Double.valueOf("10000.00").compareTo(response.getFinancialSummary().getTotalInputCosts()));
            assertEquals(0, Double.valueOf("25000.00").compareTo(response.getFinancialSummary().getTotalRevenue()));
            assertEquals(0, Double.valueOf("15000.00").compareTo(response.getFinancialSummary().getProfitLoss()));
            assertEquals(150.0, response.getFinancialSummary().getProfitMargin());
        }

        @Test
        @DisplayName("Should include upcoming activities in dashboard")
        void shouldIncludeUpcomingActivitiesInDashboard() {
            // Given
            LocalDate today = LocalDate.now();
            LocalDate nextMonth = today.plusMonths(1);
            
            Crop cropWithUpcomingHarvest = Crop.builder()
                    .id(1L)
                    .farm(testFarm)
                    .cropName("Paddy")
                    .sowingDate(today.minusDays(60))
                    .expectedHarvestDate(today.plusDays(15))
                    .areaAcres(2.5)
                    .status(Crop.CropStatus.GROWING)
                    .build();

            when(userRepository.findByFarmerId(testUser.getFarmerId())).thenReturn(Optional.of(testUser));
            when(farmRepository.findByUserIdAndIsActiveTrue(testUser.getId())).thenReturn(List.of());
            when(cropRepository.findCurrentCropsByUserId(testUser.getId())).thenReturn(List.of(cropWithUpcomingHarvest));
            when(cropRepository.findUpcomingHarvests(eq(testUser.getId()), eq(today), eq(nextMonth)))
                    .thenReturn(List.of(cropWithUpcomingHarvest));
            when(cropRepository.findByFarmUserIdAndStatus(testUser.getId(), Crop.CropStatus.GROWING))
                    .thenReturn(List.of(cropWithUpcomingHarvest));
            when(fertilizerApplicationRepository.countByCropId(1L)).thenReturn(0L);
            when(cropRepository.calculateTotalInputCost(eq(testUser.getId()), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(0.0);
            when(cropRepository.calculateTotalRevenue(eq(testUser.getId()), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(0.0);
            when(profileVersionRepository.findRecentByUserId(eq(testUser.getId()), any(PageRequest.class)))
                    .thenReturn(List.of());
            when(livestockRepository.findByUserIdAndIsActiveTrue(testUser.getId())).thenReturn(List.of());
            when(equipmentRepository.findByUserIdAndIsActiveTrue(testUser.getId())).thenReturn(List.of());

            // When
            ProfileDashboardResponse response = profileDashboardService.getDashboard(testUser.getFarmerId());

            // Then
            assertNotNull(response.getUpcomingActivities());
            assertFalse(response.getUpcomingActivities().isEmpty());
            
            // Verify harvest activity is included
            boolean hasHarvestActivity = response.getUpcomingActivities().stream()
                    .anyMatch(a -> "HARVEST".equals(a.getActivityType()) && "Paddy".equals(a.getCropName()));
            assertTrue(hasHarvestActivity);
            
            // Verify fertilizer activity is included (no applications yet)
            boolean hasFertilizerActivity = response.getUpcomingActivities().stream()
                    .anyMatch(a -> "FERTILIZER".equals(a.getActivityType()) && "Paddy".equals(a.getCropName()));
            assertTrue(hasFertilizerActivity);
        }

        @Test
        @DisplayName("Should handle empty dashboard gracefully")
        void shouldHandleEmptyDashboardGracefully() {
            // Given
            when(userRepository.findByFarmerId(testUser.getFarmerId())).thenReturn(Optional.of(testUser));
            when(farmRepository.findByUserIdAndIsActiveTrue(testUser.getId())).thenReturn(List.of());
            when(cropRepository.findCurrentCropsByUserId(testUser.getId())).thenReturn(List.of());
            when(cropRepository.findUpcomingHarvests(eq(testUser.getId()), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(List.of());
            when(cropRepository.findByFarmUserIdAndStatus(testUser.getId(), Crop.CropStatus.GROWING))
                    .thenReturn(List.of());
            when(cropRepository.calculateTotalInputCost(eq(testUser.getId()), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(0.0);
            when(cropRepository.calculateTotalRevenue(eq(testUser.getId()), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(0.0);
            when(profileVersionRepository.findRecentByUserId(eq(testUser.getId()), any(PageRequest.class)))
                    .thenReturn(List.of());
            when(livestockRepository.findByUserIdAndIsActiveTrue(testUser.getId())).thenReturn(List.of());
            when(equipmentRepository.findByUserIdAndIsActiveTrue(testUser.getId())).thenReturn(List.of());

            // When
            ProfileDashboardResponse response = profileDashboardService.getDashboard(testUser.getFarmerId());

            // Then
            assertNotNull(response);
            assertEquals(0, response.getFarmSummary().getTotalParcels());
            assertEquals(0.0, response.getFarmSummary().getTotalAreaAcres());
            assertTrue(response.getCurrentCrops().isEmpty());
            assertTrue(response.getUpcomingActivities().isEmpty());
            assertEquals(0.0, response.getFinancialSummary().getTotalInputCosts());
            assertEquals(0.0, response.getFinancialSummary().getTotalRevenue());
        }

        @Test
        @DisplayName("Should throw exception for non-existent user in dashboard")
        void shouldThrowExceptionForNonExistentUserInDashboard() {
            // Given
            when(userRepository.findByFarmerId("NON-EXISTENT")).thenReturn(Optional.empty());

            // When & Then
            assertThrows(Exception.class, () -> profileDashboardService.getDashboard("NON-EXISTENT"));
        }
    }
}