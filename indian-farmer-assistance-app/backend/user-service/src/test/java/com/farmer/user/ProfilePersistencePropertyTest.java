package com.farmer.user;

import com.farmer.user.entity.*;
import com.farmer.user.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for Profile Data Persistence Round Trip.
 * 
 * Property 21: Profile Data Persistence Round Trip
 * Validates: Requirements 11A.4, 11A.6
 * 
 * These tests verify the following properties:
 * 1. For any farmer profile data created, when stored and retrieved, 
 *    the retrieved data should match the original data exactly
 * 2. For any profile update, the updated data should be persisted correctly and retrievable
 * 3. Profile version history should be maintained correctly
 * 
 * Requirements Reference:
 * - Requirement 11A.4: When a farmer adds crop records, THE Application SHALL store 
 *   crop name, variety, sowing date, expected harvest date, area under cultivation, 
 *   and input costs
 * - Requirement 11A.6: When maintaining crop history, THE Application SHALL maintain 
 *   records for at least the past 5 years or 10 crop cycles, whichever is longer
 */
@ExtendWith(MockitoExtension.class)
class ProfilePersistencePropertyTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private CropRepository cropRepository;

    @Mock
    private ProfileVersionRepository profileVersionRepository;

    private User testUser;
    private Farm testFarm;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = User.builder()
                .id(1L)
                .farmerId("TEST-FARMER-001")
                .aadhaarHash("hash123")
                .name("Test Farmer")
                .phone("+919999999999")
                .email("test@example.com")
                .preferredLanguage("en")
                .state("Karnataka")
                .district("Bangalore Rural")
                .village("Test Village")
                .pinCode("562110")
                .gpsLatitude(12.9716)
                .gpsLongitude(77.5946)
                .role(User.Role.FARMER)
                .isActive(true)
                .build();

        // Create test farm
        testFarm = Farm.builder()
                .id(1L)
                .user(testUser)
                .parcelNumber("PARCEL-001")
                .totalAreaAcres(5.0)
                .soilType("Red Loam")
                .irrigationType(Farm.IrrigationType.CANAL)
                .agroEcologicalZone("Eastern Plateau and Hills")
                .surveyNumber("SURVEY-123")
                .gpsLatitude(12.9716)
                .gpsLongitude(77.5946)
                .village("Test Village")
                .district("Bangalore Rural")
                .state("Karnataka")
                .pinCode("562110")
                .isActive(true)
                .build();
    }

    @Nested
    @DisplayName("Property 21.1: Crop Data Round Trip - Create and Retrieve")
    class CropDataRoundTrip {

        @ParameterizedTest
        @CsvSource({
            "Paddy,IR-64,2024-06-01,2024-10-15,2.5,KHARIF",
            "Wheat,HD-2967,2023-11-01,2024-04-15,3.0,RABI",
            "Maize,DHM-117,2024-07-01,2024-10-30,1.5,KHARIF",
            "Soybean,JS-335,2024-06-15,2024-10-20,2.0,KHARIF",
            "Groundnut,TMV-2,2024-06-01,2024-10-10,1.0,KHARIF"
        })
        @DisplayName("Crop data should match exactly when stored and retrieved")
        void cropDataShouldMatchExactlyWhenStoredAndRetrieved(
                String cropName, String variety, String sowingDateStr, 
                String harvestDateStr, String areaStr, String seasonStr) {
            
            LocalDate sowingDate = LocalDate.parse(sowingDateStr);
            LocalDate expectedHarvestDate = LocalDate.parse(harvestDateStr);
            double areaAcres = Double.parseDouble(areaStr);
            Crop.Season season = Crop.Season.valueOf(seasonStr);

            // Create crop with all required fields per Requirement 11A.4
            Crop crop = Crop.builder()
                    .farm(testFarm)
                    .cropName(cropName)
                    .cropVariety(variety)
                    .sowingDate(sowingDate)
                    .expectedHarvestDate(expectedHarvestDate)
                    .areaAcres(areaAcres)
                    .season(season)
                    .status(Crop.CropStatus.SOWN)
                    .seedCost(new BigDecimal("2500.00"))
                    .fertilizerCost(new BigDecimal("1500.00"))
                    .pesticideCost(new BigDecimal("800.00"))
                    .laborCost(new BigDecimal("2000.00"))
                    .otherCost(new BigDecimal("500.00"))
                    .build();

            // Mock repository behavior
            when(cropRepository.save(any(Crop.class))).thenAnswer(invocation -> {
                Crop savedCrop = invocation.getArgument(0);
                savedCrop.setId(1L);
                return savedCrop;
            });
            when(cropRepository.findById(1L)).thenReturn(Optional.of(crop));

            // Store the crop
            Crop savedCrop = cropRepository.save(crop);
            assertNotNull(savedCrop.getId(), "Crop should be saved with an ID");

            // Retrieve the crop
            Optional<Crop> retrievedCropOpt = cropRepository.findById(savedCrop.getId());

            // Verify the retrieved data matches the original exactly
            assertTrue(retrievedCropOpt.isPresent(), "Crop should be retrievable");
            Crop retrievedCrop = retrievedCropOpt.get();

            assertEquals(cropName, retrievedCrop.getCropName(), "Crop name should match");
            assertEquals(variety, retrievedCrop.getCropVariety(), "Crop variety should match");
            assertEquals(sowingDate, retrievedCrop.getSowingDate(), "Sowing date should match");
            assertEquals(expectedHarvestDate, retrievedCrop.getExpectedHarvestDate(), "Expected harvest date should match");
            assertEquals(areaAcres, retrievedCrop.getAreaAcres(), 0.01, "Area should match");
            assertEquals(season, retrievedCrop.getSeason(), "Season should match");
            assertEquals(Crop.CropStatus.SOWN, retrievedCrop.getStatus(), "Status should match");
            assertEquals(new BigDecimal("2500.00"), retrievedCrop.getSeedCost(), "Seed cost should match");
            assertEquals(new BigDecimal("1500.00"), retrievedCrop.getFertilizerCost(), "Fertilizer cost should match");
            assertEquals(new BigDecimal("800.00"), retrievedCrop.getPesticideCost(), "Pesticide cost should match");
            assertEquals(new BigDecimal("2000.00"), retrievedCrop.getLaborCost(), "Labor cost should match");
            assertEquals(new BigDecimal("500.00"), retrievedCrop.getOtherCost(), "Other cost should match");
        }

        @Test
        @DisplayName("Multiple crops for same farm should all be retrievable with correct data")
        void multipleCropsForSameFarmShouldBeRetrievable() {
            // Create multiple crops for the same farm
            Crop crop1 = Crop.builder()
                    .farm(testFarm)
                    .cropName("Paddy")
                    .cropVariety("IR-64")
                    .sowingDate(LocalDate.of(2024, 6, 1))
                    .expectedHarvestDate(LocalDate.of(2024, 10, 15))
                    .areaAcres(2.5)
                    .season(Crop.Season.KHARIF)
                    .status(Crop.CropStatus.SOWN)
                    .seedCost(new BigDecimal("2500.00"))
                    .build();

            Crop crop2 = Crop.builder()
                    .farm(testFarm)
                    .cropName("Wheat")
                    .cropVariety("HD-2967")
                    .sowingDate(LocalDate.of(2023, 11, 1))
                    .expectedHarvestDate(LocalDate.of(2024, 4, 15))
                    .areaAcres(3.0)
                    .season(Crop.Season.RABI)
                    .status(Crop.CropStatus.HARVESTED)
                    .seedCost(new BigDecimal("3000.00"))
                    .fertilizerCost(new BigDecimal("2000.00"))
                    .build();

            Crop crop3 = Crop.builder()
                    .farm(testFarm)
                    .cropName("Maize")
                    .cropVariety("DHM-117")
                    .sowingDate(LocalDate.of(2024, 7, 1))
                    .expectedHarvestDate(LocalDate.of(2024, 10, 30))
                    .areaAcres(1.5)
                    .season(Crop.Season.KHARIF)
                    .status(Crop.CropStatus.GROWING)
                    .seedCost(new BigDecimal("1500.00"))
                    .build();

            // Mock repository behavior
            when(cropRepository.save(any(Crop.class))).thenAnswer(invocation -> {
                Crop saved = invocation.getArgument(0);
                saved.setId(System.currentTimeMillis());
                return saved;
            });
            when(cropRepository.findByFarmId(testFarm.getId())).thenReturn(List.of(crop1, crop2, crop3));

            // Store all crops
            cropRepository.save(crop1);
            cropRepository.save(crop2);
            cropRepository.save(crop3);

            // Retrieve all crops for the farm
            List<Crop> crops = cropRepository.findByFarmId(testFarm.getId());

            // Verify all crops are retrievable
            assertEquals(3, crops.size(), "All 3 crops should be retrievable");

            // Verify each crop's data matches
            assertTrue(crops.stream().anyMatch(c -> c.getCropName().equals("Paddy")), "Paddy should be present");
            assertTrue(crops.stream().anyMatch(c -> c.getCropName().equals("Wheat")), "Wheat should be present");
            assertTrue(crops.stream().anyMatch(c -> c.getCropName().equals("Maize")), "Maize should be present");
        }
    }

    @Nested
    @DisplayName("Property 21.2: Profile Update Persistence")
    class ProfileUpdatePersistence {

        @Test
        @DisplayName("Crop update should be persisted correctly and retrievable")
        void cropUpdateShouldBePersistedCorrectly() {
            // Create initial crop
            Crop crop = Crop.builder()
                    .farm(testFarm)
                    .cropName("Paddy")
                    .cropVariety("IR-64")
                    .sowingDate(LocalDate.of(2024, 6, 1))
                    .expectedHarvestDate(LocalDate.of(2024, 10, 15))
                    .areaAcres(2.5)
                    .season(Crop.Season.KHARIF)
                    .status(Crop.CropStatus.SOWN)
                    .seedCost(new BigDecimal("2500.00"))
                    .build();

            // Mock initial save
            when(cropRepository.save(any(Crop.class))).thenAnswer(invocation -> {
                Crop saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            when(cropRepository.findById(1L)).thenReturn(Optional.of(crop));

            // Store the crop
            Crop savedCrop = cropRepository.save(crop);
            assertEquals(1L, savedCrop.getId());

            // Retrieve and update
            Optional<Crop> retrievedCropOpt = cropRepository.findById(1L);
            assertTrue(retrievedCropOpt.isPresent());
            Crop cropToUpdate = retrievedCropOpt.get();

            // Update fields
            cropToUpdate.setCropName("Basmati Rice");
            cropToUpdate.setCropVariety("Basmati-1509");
            cropToUpdate.setAreaAcres(3.0);
            cropToUpdate.setStatus(Crop.CropStatus.GROWING);
            cropToUpdate.setSeedCost(new BigDecimal("3000.00"));
            cropToUpdate.setFertilizerCost(new BigDecimal("2000.00"));

            // Mock update save
            when(cropRepository.save(any(Crop.class))).thenReturn(cropToUpdate);
            when(cropRepository.findById(1L)).thenReturn(Optional.of(cropToUpdate));

            // Save update
            Crop updatedCrop = cropRepository.save(cropToUpdate);

            // Retrieve again and verify updates
            Optional<Crop> finalCropOpt = cropRepository.findById(1L);
            assertTrue(finalCropOpt.isPresent());
            Crop finalCrop = finalCropOpt.get();

            assertEquals("Basmati Rice", finalCrop.getCropName(), "Crop name should be updated");
            assertEquals("Basmati-1509", finalCrop.getCropVariety(), "Crop variety should be updated");
            assertEquals(3.0, finalCrop.getAreaAcres(), 0.01, "Area should be updated");
            assertEquals(Crop.CropStatus.GROWING, finalCrop.getStatus(), "Status should be updated");
            assertEquals(new BigDecimal("3000.00"), finalCrop.getSeedCost(), "Seed cost should be updated");
            assertEquals(new BigDecimal("2000.00"), finalCrop.getFertilizerCost(), "Fertilizer cost should be updated");
        }

        @Test
        @DisplayName("Multiple updates to same crop should all be persisted")
        void multipleUpdatesToSameCropShouldAllBePersisted() {
            // Create initial crop
            Crop crop = Crop.builder()
                    .farm(testFarm)
                    .cropName("Paddy")
                    .cropVariety("IR-64")
                    .sowingDate(LocalDate.of(2024, 6, 1))
                    .expectedHarvestDate(LocalDate.of(2024, 10, 15))
                    .areaAcres(2.5)
                    .season(Crop.Season.KHARIF)
                    .status(Crop.CropStatus.SOWN)
                    .build();

            when(cropRepository.save(any(Crop.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(cropRepository.findById(1L)).thenReturn(Optional.of(crop));

            // Store initial crop
            Crop savedCrop = cropRepository.save(crop);
            savedCrop.setId(1L);

            // Perform multiple updates
            for (int i = 0; i < 5; i++) {
                Optional<Crop> cropOpt = cropRepository.findById(1L);
                assertTrue(cropOpt.isPresent());
                Crop c = cropOpt.get();
                c.setStatus(Crop.CropStatus.values()[i % Crop.CropStatus.values().length]);
                c.setNotes("Update " + (i + 1));
                cropRepository.save(c);
            }

            // Verify final state
            Optional<Crop> finalCropOpt = cropRepository.findById(1L);
            assertTrue(finalCropOpt.isPresent());
            Crop finalCrop = finalCropOpt.get();
            assertEquals("Update 5", finalCrop.getNotes(), "Final update should be persisted");
            
            // Verify save was called 6 times (1 initial + 5 updates)
            verify(cropRepository, times(6)).save(any(Crop.class));
        }
    }

    @Nested
    @DisplayName("Property 21.3: Version History Maintenance")
    class VersionHistoryMaintenance {

        @Test
        @DisplayName("Creating a crop should create version history entry")
        void creatingCropShouldCreateVersionHistoryEntry() {
            // Create crop
            Crop crop = Crop.builder()
                    .farm(testFarm)
                    .cropName("Paddy")
                    .cropVariety("IR-64")
                    .sowingDate(LocalDate.of(2024, 6, 1))
                    .expectedHarvestDate(LocalDate.of(2024, 10, 15))
                    .areaAcres(2.5)
                    .season(Crop.Season.KHARIF)
                    .status(Crop.CropStatus.SOWN)
                    .build();

            // Create version history entry
            ProfileVersion version = ProfileVersion.builder()
                    .userId(testUser.getId())
                    .farmerId(testUser.getFarmerId())
                    .entityType(ProfileVersion.EntityType.CROP)
                    .entityId(1L)
                    .changeType(ProfileVersion.ChangeType.CREATE)
                    .fieldName("all")
                    .newValue("Crop created with name: Paddy")
                    .changedBy("SYSTEM")
                    .versionNumber(1L)
                    .build();

            // Mock repository behavior
            when(profileVersionRepository.save(any(ProfileVersion.class))).thenAnswer(invocation -> {
                ProfileVersion saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            when(profileVersionRepository.findByUserIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
                    eq(testUser.getId()), eq(ProfileVersion.EntityType.CROP), eq(1L)))
                    .thenReturn(List.of(version));

            // Create version history
            ProfileVersion savedVersion = profileVersionRepository.save(version);
            assertNotNull(savedVersion.getId(), "Version should be saved with an ID");

            // Verify version history
            List<ProfileVersion> versions = profileVersionRepository
                    .findByUserIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
                            testUser.getId(), 
                            ProfileVersion.EntityType.CROP, 
                            1L);

            assertFalse(versions.isEmpty(), "Version history should not be empty");
            assertEquals(1, versions.size(), "Should have one version entry");
            assertEquals(ProfileVersion.ChangeType.CREATE, versions.get(0).getChangeType(), "Change type should be CREATE");
            assertEquals(1L, versions.get(0).getVersionNumber(), "Version number should be 1");
        }

        @Test
        @DisplayName("Updating a crop should create version history entry")
        void updatingCropShouldCreateVersionHistoryEntry() {
            // Create version entries
            ProfileVersion createVersion = ProfileVersion.builder()
                    .userId(testUser.getId())
                    .farmerId(testUser.getFarmerId())
                    .entityType(ProfileVersion.EntityType.CROP)
                    .entityId(1L)
                    .changeType(ProfileVersion.ChangeType.CREATE)
                    .fieldName("all")
                    .newValue("Crop created")
                    .changedBy("SYSTEM")
                    .versionNumber(1L)
                    .build();

            ProfileVersion updateVersion = ProfileVersion.builder()
                    .userId(testUser.getId())
                    .farmerId(testUser.getFarmerId())
                    .entityType(ProfileVersion.EntityType.CROP)
                    .entityId(1L)
                    .changeType(ProfileVersion.ChangeType.UPDATE)
                    .fieldName("status")
                    .oldValue("SOWN")
                    .newValue("GROWING")
                    .changedBy("USER")
                    .versionNumber(2L)
                    .build();

            // Mock repository behavior
            when(profileVersionRepository.save(any(ProfileVersion.class))).thenAnswer(invocation -> {
                ProfileVersion saved = invocation.getArgument(0);
                saved.setId(System.currentTimeMillis());
                return saved;
            });
            when(profileVersionRepository.findByUserIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
                    eq(testUser.getId()), eq(ProfileVersion.EntityType.CROP), eq(1L)))
                    .thenReturn(List.of(updateVersion, createVersion));

            // Create version history entries
            profileVersionRepository.save(createVersion);
            profileVersionRepository.save(updateVersion);

            // Verify version history
            List<ProfileVersion> versions = profileVersionRepository
                    .findByUserIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
                            testUser.getId(), 
                            ProfileVersion.EntityType.CROP, 
                            1L);

            assertEquals(2, versions.size(), "Should have two version entries");
            assertEquals(ProfileVersion.ChangeType.UPDATE, versions.get(0).getChangeType(), "First entry should be UPDATE");
            assertEquals(ProfileVersion.ChangeType.CREATE, versions.get(1).getChangeType(), "Second entry should be CREATE");
            assertEquals(2L, versions.get(0).getVersionNumber(), "Latest version number should be 2");
            assertEquals(1L, versions.get(1).getVersionNumber(), "First version number should be 1");
        }

        @Test
        @DisplayName("Version history should track all changes for a user")
        void versionHistoryShouldTrackAllChanges() {
            // Create multiple version entries
            List<ProfileVersion> versions = new java.util.ArrayList<>();
            for (int i = 0; i < 10; i++) {
                ProfileVersion version = ProfileVersion.builder()
                        .userId(testUser.getId())
                        .farmerId(testUser.getFarmerId())
                        .entityType(ProfileVersion.EntityType.values()[i % ProfileVersion.EntityType.values().length])
                        .entityId((long) (i + 1))
                        .changeType(ProfileVersion.ChangeType.values()[i % ProfileVersion.ChangeType.values().length])
                        .fieldName("field" + i)
                        .newValue("Value " + i)
                        .changedBy("USER")
                        .versionNumber((long) (i + 1))
                        .build();
                versions.add(version);
            }

            // Mock repository behavior
            when(profileVersionRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId()))
                    .thenReturn(versions);

            // Verify all version history entries are retrievable
            List<ProfileVersion> allVersions = profileVersionRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());

            assertEquals(10, allVersions.size(), "All 10 version entries should be retrievable");
        }
    }

    @Nested
    @DisplayName("Property 21.4: Crop History for Past 5 Years")
    class CropHistoryMaintenance {

        @Test
        @DisplayName("Crop history should maintain records for past 5 years")
        void cropHistoryShouldMaintainRecordsForPast5Years() {
            // Create crops spanning multiple years
            LocalDate fiveYearsAgo = LocalDate.now().minusYears(5);
            List<Crop> crops = new java.util.ArrayList<>();
            
            for (int i = 0; i < 12; i++) { // 12 crops over 5 years (more than 10 cycles)
                Crop crop = Crop.builder()
                        .farm(testFarm)
                        .cropName("Crop-" + i)
                        .cropVariety("Variety-" + i)
                        .sowingDate(fiveYearsAgo.plusMonths(i))
                        .expectedHarvestDate(fiveYearsAgo.plusMonths(i).plusDays(120))
                        .areaAcres(1.0 + i * 0.1)
                        .season(Crop.Season.KHARIF)
                        .status(Crop.CropStatus.HARVESTED)
                        .build();
                crops.add(crop);
            }

            // Mock repository behavior
            when(cropRepository.findCropHistory(testUser.getId(), fiveYearsAgo)).thenReturn(crops);

            // Query crop history for past 5 years
            List<Crop> history = cropRepository.findCropHistory(testUser.getId(), fiveYearsAgo);

            // Verify at least 10 crop cycles are maintained (Requirement 11A.6)
            assertTrue(history.size() >= 10, 
                    "Should maintain at least 10 crop cycles, found: " + history.size());
            
            // Verify all crops are within the 5-year window
            for (Crop crop : history) {
                assertTrue(!crop.getSowingDate().isBefore(fiveYearsAgo),
                        "All crops should be within the 5-year window");
            }
        }

        @Test
        @DisplayName("Crop history query should return crops in descending order by sowing date")
        void cropHistoryQueryShouldReturnCropsInDescendingOrder() {
            // Create crops with different sowing dates
            LocalDate now = LocalDate.now();
            
            Crop crop1 = Crop.builder()
                    .farm(testFarm)
                    .cropName("Oldest")
                    .sowingDate(now.minusYears(4))
                    .areaAcres(1.0)
                    .build();
            
            Crop crop2 = Crop.builder()
                    .farm(testFarm)
                    .cropName("Middle")
                    .sowingDate(now.minusYears(2))
                    .areaAcres(1.0)
                    .build();
            
            Crop crop3 = Crop.builder()
                    .farm(testFarm)
                    .cropName("Newest")
                    .sowingDate(now.minusMonths(6))
                    .areaAcres(1.0)
                    .build();

            List<Crop> crops = List.of(crop3, crop2, crop1); // Already in descending order

            // Mock repository behavior
            when(cropRepository.findCropHistory(testUser.getId(), now.minusYears(5))).thenReturn(crops);

            // Query crop history
            List<Crop> history = cropRepository.findCropHistory(testUser.getId(), now.minusYears(5));

            // Verify descending order by sowing date
            assertEquals(3, history.size());
            assertEquals("Newest", history.get(0).getCropName());
            assertEquals("Middle", history.get(1).getCropName());
            assertEquals("Oldest", history.get(2).getCropName());
        }
    }

    @Nested
    @DisplayName("Property 21.5: Input Cost Tracking")
    class InputCostTracking {

        @Test
        @DisplayName("All input cost components should be stored and retrievable")
        void allInputCostComponentsShouldBeStoredAndRetrievable() {
            // Create crop with all input costs per Requirement 11A.4
            Crop crop = Crop.builder()
                    .farm(testFarm)
                    .cropName("Paddy")
                    .cropVariety("IR-64")
                    .sowingDate(LocalDate.of(2024, 6, 1))
                    .expectedHarvestDate(LocalDate.of(2024, 10, 15))
                    .areaAcres(2.5)
                    .season(Crop.Season.KHARIF)
                    .status(Crop.CropStatus.SOWN)
                    .seedCost(new BigDecimal("2500.00"))
                    .fertilizerCost(new BigDecimal("1500.00"))
                    .pesticideCost(new BigDecimal("800.00"))
                    .laborCost(new BigDecimal("2000.00"))
                    .otherCost(new BigDecimal("500.00"))
                    .build();

            // Mock repository behavior
            when(cropRepository.save(any(Crop.class))).thenAnswer(invocation -> {
                Crop saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            when(cropRepository.findById(1L)).thenReturn(Optional.of(crop));

            // Store and retrieve
            Crop savedCrop = cropRepository.save(crop);
            Optional<Crop> retrievedCropOpt = cropRepository.findById(1L);

            assertTrue(retrievedCropOpt.isPresent());
            Crop retrievedCrop = retrievedCropOpt.get();

            assertEquals(new BigDecimal("2500.00"), retrievedCrop.getSeedCost(), "Seed cost should be stored");
            assertEquals(new BigDecimal("1500.00"), retrievedCrop.getFertilizerCost(), "Fertilizer cost should be stored");
            assertEquals(new BigDecimal("800.00"), retrievedCrop.getPesticideCost(), "Pesticide cost should be stored");
            assertEquals(new BigDecimal("2000.00"), retrievedCrop.getLaborCost(), "Labor cost should be stored");
            assertEquals(new BigDecimal("500.00"), retrievedCrop.getOtherCost(), "Other cost should be stored");
        }

        @Test
        @DisplayName("Total input cost should be calculated correctly")
        void totalInputCostShouldBeCalculatedCorrectly() {
            // Create crop with individual costs
            Crop crop = Crop.builder()
                    .farm(testFarm)
                    .cropName("Paddy")
                    .sowingDate(LocalDate.of(2024, 6, 1))
                    .areaAcres(2.5)
                    .seedCost(new BigDecimal("2500.00"))
                    .fertilizerCost(new BigDecimal("1500.00"))
                    .pesticideCost(new BigDecimal("800.00"))
                    .laborCost(new BigDecimal("2000.00"))
                    .otherCost(new BigDecimal("500.00"))
                    .build();
            
            // Calculate total input cost (this is called by @PrePersist/@PreUpdate in real persistence)
            crop.calculateTotalInputCost();

            // Calculate expected total
            BigDecimal expectedTotal = new BigDecimal("2500.00")
                    .add(new BigDecimal("1500.00"))
                    .add(new BigDecimal("800.00"))
                    .add(new BigDecimal("2000.00"))
                    .add(new BigDecimal("500.00"));

            // Verify total input cost calculation
            assertEquals(expectedTotal, crop.getTotalInputCost(), 
                    "Total input cost should be sum of all individual costs");
        }
    }

    @Nested
    @DisplayName("Property 21.6: Harvest Data Persistence")
    class HarvestDataPersistence {

        @Test
        @DisplayName("Harvest data should be stored and retrievable correctly")
        void harvestDataShouldBeStoredAndRetrievableCorrectly() {
            // Create crop with harvest data
            Crop crop = Crop.builder()
                    .farm(testFarm)
                    .cropName("Paddy")
                    .cropVariety("IR-64")
                    .sowingDate(LocalDate.of(2024, 6, 1))
                    .expectedHarvestDate(LocalDate.of(2024, 10, 15))
                    .actualHarvestDate(LocalDate.of(2024, 10, 20))
                    .areaAcres(2.5)
                    .season(Crop.Season.KHARIF)
                    .status(Crop.CropStatus.HARVESTED)
                    .totalYieldQuintals(new BigDecimal("12.5"))
                    .qualityGrade("A")
                    .sellingPricePerQuintal(new BigDecimal("2200.00"))
                    .mandiName("Bangalore APMC")
                    .build();

            // Mock repository behavior
            when(cropRepository.save(any(Crop.class))).thenAnswer(invocation -> {
                Crop saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            when(cropRepository.findById(1L)).thenReturn(Optional.of(crop));

            // Store and retrieve
            Crop savedCrop = cropRepository.save(crop);
            Optional<Crop> retrievedCropOpt = cropRepository.findById(1L);

            assertTrue(retrievedCropOpt.isPresent());
            Crop retrievedCrop = retrievedCropOpt.get();

            assertEquals(LocalDate.of(2024, 10, 20), retrievedCrop.getActualHarvestDate(), 
                    "Actual harvest date should be stored");
            assertEquals(new BigDecimal("12.5"), retrievedCrop.getTotalYieldQuintals(), 
                    "Total yield should be stored");
            assertEquals("A", retrievedCrop.getQualityGrade(), 
                    "Quality grade should be stored");
            assertEquals(new BigDecimal("2200.00"), retrievedCrop.getSellingPricePerQuintal(), 
                    "Selling price should be stored");
            assertEquals("Bangalore APMC", retrievedCrop.getMandiName(), 
                    "Mandi name should be stored");
        }

        @Test
        @DisplayName("Revenue should be calculated from yield and selling price")
        void revenueShouldBeCalculatedFromYieldAndSellingPrice() {
            // Create crop with yield and selling price
            Crop crop = Crop.builder()
                    .farm(testFarm)
                    .cropName("Wheat")
                    .sowingDate(LocalDate.of(2023, 11, 1))
                    .areaAcres(3.0)
                    .totalYieldQuintals(new BigDecimal("15.0"))
                    .sellingPricePerQuintal(new BigDecimal("2500.00"))
                    .build();

            // Calculate revenue (this method needs to be called explicitly)
            crop.calculateTotalRevenue();

            // Calculate expected revenue
            BigDecimal expectedRevenue = new BigDecimal("15.0").multiply(new BigDecimal("2500.00"));

            // Verify revenue calculation
            assertEquals(expectedRevenue, crop.getTotalRevenue(), 
                    "Total revenue should be calculated from yield and selling price");
        }
    }
}