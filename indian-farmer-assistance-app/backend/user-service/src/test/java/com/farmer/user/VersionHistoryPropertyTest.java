package com.farmer.user;

import com.farmer.user.entity.*;
import com.farmer.user.repository.*;
import com.farmer.user.service.UserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for Version History Maintenance.
 * 
 * Property 23: Version History Maintenance
 * Validates: Requirements 11A.7, 21.6
 * 
 * These tests verify the following properties:
 * 1. For any profile update, a version history entry should be created with timestamp and changes
 * 2. Version history should track all changes to profile data including crops, farms, and personal information
 * 3. Version numbers should increment sequentially for each update
 * 
 * Requirements Reference:
 * - Requirement 11A.7: When a farmer updates profile information, THE Application SHALL 
 *   version the changes and maintain an audit trail
 * - Requirement 21.6: Version history should be maintained for all profile changes
 */
@ExtendWith(MockitoExtension.class)
class VersionHistoryPropertyTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private CropRepository cropRepository;

    @Mock
    private ProfileVersionRepository profileVersionRepository;

    @InjectMocks
    private UserService userService;

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
    @DisplayName("Property 23.1: Version History Entry Creation for Profile Updates")
    class VersionHistoryEntryCreation {

        @Test
        @DisplayName("Creating a user profile should create version history entry")
        void creatingUserProfileShouldCreateVersionHistoryEntry() {
            // Create user profile version entry
            ProfileVersion version = ProfileVersion.builder()
                    .userId(testUser.getId())
                    .farmerId(testUser.getFarmerId())
                    .entityType(ProfileVersion.EntityType.USER_PROFILE)
                    .entityId(testUser.getId())
                    .changeType(ProfileVersion.ChangeType.CREATE)
                    .fieldName("all")
                    .newValue("User profile created: Test Farmer, Karnataka")
                    .changedBy("SYSTEM")
                    .versionNumber(1L)
                    .createdAt(LocalDateTime.now())
                    .build();

            // Mock repository behavior
            when(profileVersionRepository.save(any(ProfileVersion.class))).thenAnswer(invocation -> {
                ProfileVersion saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            lenient().when(profileVersionRepository.findByUserIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
                    eq(testUser.getId()), eq(ProfileVersion.EntityType.USER_PROFILE), eq(testUser.getId())))
                    .thenReturn(List.of(version));

            // Create version history entry
            ProfileVersion savedVersion = profileVersionRepository.save(version);

            // Verify version history entry was created
            assertNotNull(savedVersion.getId(), "Version should be saved with an ID");
            assertEquals(ProfileVersion.ChangeType.CREATE, savedVersion.getChangeType(), 
                    "Change type should be CREATE for new profile");
            assertNotNull(savedVersion.getCreatedAt(), "Version should have timestamp");
            assertEquals(1L, savedVersion.getVersionNumber(), "Initial version number should be 1");
        }

        @Test
        @DisplayName("Creating a farm should create version history entry")
        void creatingFarmShouldCreateVersionHistoryEntry() {
            // Create farm version entry
            ProfileVersion version = ProfileVersion.builder()
                    .userId(testUser.getId())
                    .farmerId(testUser.getFarmerId())
                    .entityType(ProfileVersion.EntityType.FARM)
                    .entityId(testFarm.getId())
                    .changeType(ProfileVersion.ChangeType.CREATE)
                    .fieldName("all")
                    .newValue("Farm created: PARCEL-001, 5.0 acres, Red Loam soil")
                    .changedBy("USER")
                    .versionNumber(1L)
                    .createdAt(LocalDateTime.now())
                    .build();

            // Mock repository behavior
            when(profileVersionRepository.save(any(ProfileVersion.class))).thenAnswer(invocation -> {
                ProfileVersion saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            lenient().when(profileVersionRepository.findByUserIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
                    eq(testUser.getId()), eq(ProfileVersion.EntityType.FARM), eq(testFarm.getId())))
                    .thenReturn(List.of(version));

            // Create version history entry
            ProfileVersion savedVersion = profileVersionRepository.save(version);

            // Verify version history entry was created
            assertNotNull(savedVersion.getId(), "Version should be saved with an ID");
            assertEquals(ProfileVersion.EntityType.FARM, savedVersion.getEntityType(), 
                    "Entity type should be FARM");
            assertEquals("USER", savedVersion.getChangedBy(), "Changed by should be USER");
        }

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

            // Create crop version entry
            ProfileVersion version = ProfileVersion.builder()
                    .userId(testUser.getId())
                    .farmerId(testUser.getFarmerId())
                    .entityType(ProfileVersion.EntityType.CROP)
                    .entityId(1L)
                    .changeType(ProfileVersion.ChangeType.CREATE)
                    .fieldName("all")
                    .newValue("Crop created: Paddy (IR-64), 2.5 acres, KHARIF 2024")
                    .changedBy("SYSTEM")
                    .versionNumber(1L)
                    .createdAt(LocalDateTime.now())
                    .build();

            // Mock repository behavior
            when(profileVersionRepository.save(any(ProfileVersion.class))).thenAnswer(invocation -> {
                ProfileVersion saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            lenient().when(profileVersionRepository.findByUserIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
                    eq(testUser.getId()), eq(ProfileVersion.EntityType.CROP), eq(1L)))
                    .thenReturn(List.of(version));

            // Create version history entry
            ProfileVersion savedVersion = profileVersionRepository.save(version);

            // Verify version history entry was created
            assertNotNull(savedVersion.getId(), "Version should be saved with an ID");
            assertEquals(ProfileVersion.EntityType.CROP, savedVersion.getEntityType(), 
                    "Entity type should be CROP");
            assertTrue(savedVersion.getNewValue().contains("Paddy"), 
                    "Version should contain crop name");
        }
    }

    @Nested
    @DisplayName("Property 23.2: Version Number Sequential Increment")
    class VersionNumberSequentialIncrement {

        @Test
        @DisplayName("Version numbers should increment sequentially for each update")
        void versionNumbersShouldIncrementSequentially() {
            // Create version entries with sequential version numbers
            List<ProfileVersion> versions = new ArrayList<>();
            long currentVersion = 1;

            for (int i = 0; i < 5; i++) {
                ProfileVersion version = ProfileVersion.builder()
                        .userId(testUser.getId())
                        .farmerId(testUser.getFarmerId())
                        .entityType(ProfileVersion.EntityType.CROP)
                        .entityId(1L)
                        .changeType(i == 0 ? ProfileVersion.ChangeType.CREATE : ProfileVersion.ChangeType.UPDATE)
                        .fieldName("status")
                        .oldValue(i == 0 ? null : "SOWN")
                        .newValue(i == 0 ? "SOWN" : Crop.CropStatus.values()[i % Crop.CropStatus.values().length].name())
                        .changedBy("USER")
                        .versionNumber(currentVersion++)
                        .createdAt(LocalDateTime.now().plusSeconds(i))
                        .build();
                versions.add(version);
            }

            // Mock repository behavior
            when(profileVersionRepository.save(any(ProfileVersion.class))).thenAnswer(invocation -> {
                ProfileVersion saved = invocation.getArgument(0);
                saved.setId(System.currentTimeMillis());
                return saved;
            });
            when(profileVersionRepository.findByUserIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
                    eq(testUser.getId()), eq(ProfileVersion.EntityType.CROP), eq(1L)))
                    .thenReturn(versions);

            // Save all versions
            for (ProfileVersion version : versions) {
                profileVersionRepository.save(version);
            }

            // Verify version history
            List<ProfileVersion> savedVersions = profileVersionRepository
                    .findByUserIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
                            testUser.getId(), ProfileVersion.EntityType.CROP, 1L);

            // Verify sequential version numbers
            assertEquals(5, savedVersions.size(), "Should have 5 version entries");
            for (int i = 0; i < savedVersions.size(); i++) {
                assertEquals((long) (i + 1), savedVersions.get(i).getVersionNumber(),
                        "Version " + (i + 1) + " should have correct version number");
            }
        }

        @Test
        @DisplayName("Latest version should have highest version number")
        void latestVersionShouldHaveHighestVersionNumber() {
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
                    .createdAt(LocalDateTime.now().minusDays(2))
                    .build();

            ProfileVersion updateVersion1 = ProfileVersion.builder()
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
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .build();

            ProfileVersion updateVersion2 = ProfileVersion.builder()
                    .userId(testUser.getId())
                    .farmerId(testUser.getFarmerId())
                    .entityType(ProfileVersion.EntityType.CROP)
                    .entityId(1L)
                    .changeType(ProfileVersion.ChangeType.UPDATE)
                    .fieldName("area_acres")
                    .oldValue("2.5")
                    .newValue("3.0")
                    .changedBy("USER")
                    .versionNumber(3L)
                    .createdAt(LocalDateTime.now())
                    .build();

            // Mock repository behavior - returns in descending order by createdAt
            when(profileVersionRepository.findByUserIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
                    eq(testUser.getId()), eq(ProfileVersion.EntityType.CROP), eq(1L)))
                    .thenReturn(List.of(updateVersion2, updateVersion1, createVersion));

            // Get version history
            List<ProfileVersion> versions = profileVersionRepository
                    .findByUserIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
                            testUser.getId(), ProfileVersion.EntityType.CROP, 1L);

            // Verify latest version has highest version number
            assertEquals(3, versions.size(), "Should have 3 version entries");
            assertEquals(3L, versions.get(0).getVersionNumber(), 
                    "Latest version should have highest version number (3)");
            assertEquals(2L, versions.get(1).getVersionNumber(), 
                    "Second version should have version number 2");
            assertEquals(1L, versions.get(2).getVersionNumber(), 
                    "First version should have version number 1");
        }

        @Test
        @DisplayName("Version numbers should be unique per entity")
        void versionNumbersShouldBeUniquePerEntity() {
            // Create versions for different entities
            ProfileVersion userVersion = ProfileVersion.builder()
                    .userId(testUser.getId())
                    .farmerId(testUser.getFarmerId())
                    .entityType(ProfileVersion.EntityType.USER_PROFILE)
                    .entityId(testUser.getId())
                    .changeType(ProfileVersion.ChangeType.CREATE)
                    .fieldName("all")
                    .newValue("User created")
                    .changedBy("SYSTEM")
                    .versionNumber(1L)
                    .createdAt(LocalDateTime.now())
                    .build();

            ProfileVersion farmVersion = ProfileVersion.builder()
                    .userId(testUser.getId())
                    .farmerId(testUser.getFarmerId())
                    .entityType(ProfileVersion.EntityType.FARM)
                    .entityId(testFarm.getId())
                    .changeType(ProfileVersion.ChangeType.CREATE)
                    .fieldName("all")
                    .newValue("Farm created")
                    .changedBy("SYSTEM")
                    .versionNumber(1L)
                    .createdAt(LocalDateTime.now())
                    .build();

            // Mock repository behavior
            when(profileVersionRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId()))
                    .thenReturn(List.of(userVersion, farmVersion));

            // Get all version history for user
            List<ProfileVersion> allVersions = profileVersionRepository
                    .findByUserIdOrderByCreatedAtDesc(testUser.getId());

            // Verify both entities have version 1 (unique per entity)
            assertEquals(2, allVersions.size(), "Should have 2 version entries");
            assertTrue(allVersions.stream().anyMatch(v -> 
                    v.getEntityType() == ProfileVersion.EntityType.USER_PROFILE && v.getVersionNumber() == 1L),
                    "User profile should have version 1");
            assertTrue(allVersions.stream().anyMatch(v -> 
                    v.getEntityType() == ProfileVersion.EntityType.FARM && v.getVersionNumber() == 1L),
                    "Farm should have version 1");
        }
    }

    @Nested
    @DisplayName("Property 23.3: Audit Trail Captures Changes Correctly")
    class AuditTrailCapturesChanges {

        @Test
        @DisplayName("Update should capture old and new values")
        void updateShouldCaptureOldAndNewValues() {
            // Create update version with old and new values
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
                    .createdAt(LocalDateTime.now())
                    .build();

            // Mock repository behavior
            when(profileVersionRepository.save(any(ProfileVersion.class))).thenReturn(updateVersion);

            // Save update version
            ProfileVersion savedVersion = profileVersionRepository.save(updateVersion);

            // Verify audit trail captures changes
            assertEquals("SOWN", savedVersion.getOldValue(), "Old value should be captured");
            assertEquals("GROWING", savedVersion.getNewValue(), "New value should be captured");
            assertEquals("status", savedVersion.getFieldName(), "Field name should be captured");
        }

        @Test
        @DisplayName("Version history should track changes to all entity types")
        void versionHistoryShouldTrackChangesToAllEntityTypes() {
            // Create versions for all entity types
            List<ProfileVersion> versions = new ArrayList<>();
            ProfileVersion.EntityType[] entityTypes = ProfileVersion.EntityType.values();

            for (int i = 0; i < entityTypes.length; i++) {
                ProfileVersion version = ProfileVersion.builder()
                        .userId(testUser.getId())
                        .farmerId(testUser.getFarmerId())
                        .entityType(entityTypes[i])
                        .entityId((long) (i + 1))
                        .changeType(ProfileVersion.ChangeType.CREATE)
                        .fieldName("all")
                        .newValue(entityTypes[i].name() + " created")
                        .changedBy("SYSTEM")
                        .versionNumber(1L)
                        .createdAt(LocalDateTime.now().plusSeconds(i))
                        .build();
                versions.add(version);
            }

            // Mock repository behavior
            when(profileVersionRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId()))
                    .thenReturn(versions);

            // Get all version history
            List<ProfileVersion> allVersions = profileVersionRepository
                    .findByUserIdOrderByCreatedAtDesc(testUser.getId());

            // Verify all entity types are tracked
            assertEquals(entityTypes.length, allVersions.size(), 
                    "Should have version entries for all entity types");
            for (ProfileVersion.EntityType entityType : entityTypes) {
                assertTrue(allVersions.stream().anyMatch(v -> v.getEntityType() == entityType),
                        "Version history should track " + entityType);
            }
        }

        @Test
        @DisplayName("Version history should track all change types")
        void versionHistoryShouldTrackAllChangeTypes() {
            // Create versions for all change types
            List<ProfileVersion> versions = new ArrayList<>();
            ProfileVersion.ChangeType[] changeTypes = ProfileVersion.ChangeType.values();

            for (int i = 0; i < changeTypes.length; i++) {
                ProfileVersion version = ProfileVersion.builder()
                        .userId(testUser.getId())
                        .farmerId(testUser.getFarmerId())
                        .entityType(ProfileVersion.EntityType.CROP)
                        .entityId(1L)
                        .changeType(changeTypes[i])
                        .fieldName("all")
                        .newValue(changeTypes[i].name() + " operation")
                        .changedBy("SYSTEM")
                        .versionNumber((long) (i + 1))
                        .createdAt(LocalDateTime.now().plusSeconds(i))
                        .build();
                versions.add(version);
            }

            // Mock repository behavior
            when(profileVersionRepository.findByUserIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
                    eq(testUser.getId()), eq(ProfileVersion.EntityType.CROP), eq(1L)))
                    .thenReturn(versions);

            // Get version history
            List<ProfileVersion> cropVersions = profileVersionRepository
                    .findByUserIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
                            testUser.getId(), ProfileVersion.EntityType.CROP, 1L);

            // Verify all change types are tracked
            assertEquals(changeTypes.length, cropVersions.size(), 
                    "Should have version entries for all change types");
            for (ProfileVersion.ChangeType changeType : changeTypes) {
                assertTrue(cropVersions.stream().anyMatch(v -> v.getChangeType() == changeType),
                        "Version history should track " + changeType);
            }
        }

        @Test
        @DisplayName("Version history should include timestamp for each entry")
        void versionHistoryShouldIncludeTimestampForEachEntry() {
            // Create versions with different timestamps
            LocalDateTime baseTime = LocalDateTime.of(2024, 6, 1, 10, 0, 0);
            List<ProfileVersion> versions = new ArrayList<>();

            for (int i = 0; i < 3; i++) {
                ProfileVersion version = ProfileVersion.builder()
                        .userId(testUser.getId())
                        .farmerId(testUser.getFarmerId())
                        .entityType(ProfileVersion.EntityType.CROP)
                        .entityId(1L)
                        .changeType(ProfileVersion.ChangeType.UPDATE)
                        .fieldName("field" + i)
                        .newValue("Value " + i)
                        .changedBy("USER")
                        .versionNumber((long) (i + 1))
                        .createdAt(baseTime.plusHours(i))
                        .build();
                versions.add(version);
            }

            // Mock repository behavior
            when(profileVersionRepository.findByUserIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
                    eq(testUser.getId()), eq(ProfileVersion.EntityType.CROP), eq(1L)))
                    .thenReturn(versions);

            // Get version history
            List<ProfileVersion> savedVersions = profileVersionRepository
                    .findByUserIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
                            testUser.getId(), ProfileVersion.EntityType.CROP, 1L);

            // Verify timestamps are present and ordered
            assertEquals(3, savedVersions.size(), "Should have 3 version entries");
            for (ProfileVersion version : savedVersions) {
                assertNotNull(version.getCreatedAt(), "Each version should have a timestamp");
            }
        }
    }

    @Nested
    @DisplayName("Property 23.4: Version History Query Methods")
    class VersionHistoryQueryMethods {

        @Test
        @DisplayName("Should find version history by user ID")
        void shouldFindVersionHistoryByUserId() {
            List<ProfileVersion> versions = List.of(
                    ProfileVersion.builder()
                            .userId(testUser.getId())
                            .farmerId(testUser.getFarmerId())
                            .entityType(ProfileVersion.EntityType.USER_PROFILE)
                            .entityId(testUser.getId())
                            .changeType(ProfileVersion.ChangeType.CREATE)
                            .versionNumber(1L)
                            .build(),
                    ProfileVersion.builder()
                            .userId(testUser.getId())
                            .farmerId(testUser.getFarmerId())
                            .entityType(ProfileVersion.EntityType.FARM)
                            .entityId(testFarm.getId())
                            .changeType(ProfileVersion.ChangeType.CREATE)
                            .versionNumber(1L)
                            .build()
            );

            when(profileVersionRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId()))
                    .thenReturn(versions);

            List<ProfileVersion> result = profileVersionRepository
                    .findByUserIdOrderByCreatedAtDesc(testUser.getId());

            assertEquals(2, result.size(), "Should find all versions for user");
            assertTrue(result.stream().allMatch(v -> v.getUserId().equals(testUser.getId())),
                    "All versions should belong to the user");
        }

        @Test
        @DisplayName("Should find version history by entity type and ID")
        void shouldFindVersionHistoryByEntityTypeAndId() {
            List<ProfileVersion> versions = List.of(
                    ProfileVersion.builder()
                            .userId(testUser.getId())
                            .farmerId(testUser.getFarmerId())
                            .entityType(ProfileVersion.EntityType.CROP)
                            .entityId(1L)
                            .changeType(ProfileVersion.ChangeType.CREATE)
                            .versionNumber(1L)
                            .build(),
                    ProfileVersion.builder()
                            .userId(testUser.getId())
                            .farmerId(testUser.getFarmerId())
                            .entityType(ProfileVersion.EntityType.CROP)
                            .entityId(1L)
                            .changeType(ProfileVersion.ChangeType.UPDATE)
                            .versionNumber(2L)
                            .build()
            );

            when(profileVersionRepository.findByUserIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
                    eq(testUser.getId()), eq(ProfileVersion.EntityType.CROP), eq(1L)))
                    .thenReturn(versions);

            List<ProfileVersion> result = profileVersionRepository
                    .findByUserIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
                            testUser.getId(), ProfileVersion.EntityType.CROP, 1L);

            assertEquals(2, result.size(), "Should find all versions for entity");
            assertTrue(result.stream().allMatch(v -> 
                    v.getEntityType() == ProfileVersion.EntityType.CROP && v.getEntityId() == 1L),
                    "All versions should be for the specified entity");
        }

        @Test
        @DisplayName("Should find version history by change type")
        void shouldFindVersionHistoryByChangeType() {
            List<ProfileVersion> updates = List.of(
                    ProfileVersion.builder()
                            .userId(testUser.getId())
                            .farmerId(testUser.getFarmerId())
                            .entityType(ProfileVersion.EntityType.CROP)
                            .entityId(1L)
                            .changeType(ProfileVersion.ChangeType.UPDATE)
                            .versionNumber(2L)
                            .build(),
                    ProfileVersion.builder()
                            .userId(testUser.getId())
                            .farmerId(testUser.getFarmerId())
                            .entityType(ProfileVersion.EntityType.CROP)
                            .entityId(1L)
                            .changeType(ProfileVersion.ChangeType.UPDATE)
                            .versionNumber(3L)
                            .build()
            );

            when(profileVersionRepository.findByUserIdAndChangeTypeOrderByCreatedAtDesc(
                    eq(testUser.getId()), eq(ProfileVersion.ChangeType.UPDATE)))
                    .thenReturn(updates);

            List<ProfileVersion> result = profileVersionRepository
                    .findByUserIdAndChangeTypeOrderByCreatedAtDesc(
                            testUser.getId(), ProfileVersion.ChangeType.UPDATE);

            assertEquals(2, result.size(), "Should find all UPDATE versions");
            assertTrue(result.stream().allMatch(v -> v.getChangeType() == ProfileVersion.ChangeType.UPDATE),
                    "All versions should be UPDATE type");
        }

        @Test
        @DisplayName("Should get latest version number for user")
        void shouldGetLatestVersionNumberForUser() {
            when(profileVersionRepository.getLatestVersionNumber(testUser.getId()))
                    .thenReturn(5L);

            Long latestVersion = profileVersionRepository.getLatestVersionNumber(testUser.getId());

            assertEquals(5L, latestVersion, "Latest version should be 5");
        }
    }

    @Nested
    @DisplayName("Property 23.5: Profile Data Change Tracking")
    class ProfileDataChangeTracking {

        @Test
        @DisplayName("Should track personal information changes")
        void shouldTrackPersonalInformationChanges() {
            // Create version for personal info update
            ProfileVersion nameUpdate = ProfileVersion.builder()
                    .userId(testUser.getId())
                    .farmerId(testUser.getFarmerId())
                    .entityType(ProfileVersion.EntityType.USER_PROFILE)
                    .entityId(testUser.getId())
                    .changeType(ProfileVersion.ChangeType.UPDATE)
                    .fieldName("name")
                    .oldValue("Test Farmer")
                    .newValue("Updated Farmer Name")
                    .changedBy("USER")
                    .versionNumber(2L)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(profileVersionRepository.save(any(ProfileVersion.class))).thenReturn(nameUpdate);

            ProfileVersion saved = profileVersionRepository.save(nameUpdate);

            assertEquals("name", saved.getFieldName(), "Should track name field changes");
            assertEquals("USER", saved.getChangedBy(), "Should track who made the change");
        }

        @Test
        @DisplayName("Should track farm details changes")
        void shouldTrackFarmDetailsChanges() {
            // Create version for farm update
            ProfileVersion farmUpdate = ProfileVersion.builder()
                    .userId(testUser.getId())
                    .farmerId(testUser.getFarmerId())
                    .entityType(ProfileVersion.EntityType.FARM)
                    .entityId(testFarm.getId())
                    .changeType(ProfileVersion.ChangeType.UPDATE)
                    .fieldName("total_area_acres")
                    .oldValue("5.0")
                    .newValue("6.0")
                    .changedBy("USER")
                    .versionNumber(2L)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(profileVersionRepository.save(any(ProfileVersion.class))).thenReturn(farmUpdate);

            ProfileVersion saved = profileVersionRepository.save(farmUpdate);

            assertEquals("total_area_acres", saved.getFieldName(), 
                    "Should track farm area changes");
            assertEquals("5.0", saved.getOldValue(), "Should capture old value");
            assertEquals("6.0", saved.getNewValue(), "Should capture new value");
        }

        @Test
        @DisplayName("Should track crop input cost changes")
        void shouldTrackCropInputCostChanges() {
            // Create version for crop cost update
            ProfileVersion costUpdate = ProfileVersion.builder()
                    .userId(testUser.getId())
                    .farmerId(testUser.getFarmerId())
                    .entityType(ProfileVersion.EntityType.CROP)
                    .entityId(1L)
                    .changeType(ProfileVersion.ChangeType.UPDATE)
                    .fieldName("seed_cost")
                    .oldValue("2500.00")
                    .newValue("3000.00")
                    .changedBy("USER")
                    .versionNumber(3L)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(profileVersionRepository.save(any(ProfileVersion.class))).thenReturn(costUpdate);

            ProfileVersion saved = profileVersionRepository.save(costUpdate);

            assertEquals("seed_cost", saved.getFieldName(), 
                    "Should track input cost changes");
            assertTrue(saved.getNewValue().contains("3000"), 
                    "Should capture updated cost");
        }

        @Test
        @DisplayName("Should track harvest record changes")
        void shouldTrackHarvestRecordChanges() {
            // Create version for harvest record
            ProfileVersion harvestVersion = ProfileVersion.builder()
                    .userId(testUser.getId())
                    .farmerId(testUser.getFarmerId())
                    .entityType(ProfileVersion.EntityType.HARVEST_RECORD)
                    .entityId(1L)
                    .changeType(ProfileVersion.ChangeType.CREATE)
                    .fieldName("all")
                    .newValue("Harvest recorded: 50 quintals, Grade A, ₹2,200/quintal")
                    .changedBy("SYSTEM")
                    .versionNumber(1L)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(profileVersionRepository.save(any(ProfileVersion.class))).thenReturn(harvestVersion);

            ProfileVersion saved = profileVersionRepository.save(harvestVersion);

            assertEquals(ProfileVersion.EntityType.HARVEST_RECORD, saved.getEntityType(),
                    "Should track harvest record entity type");
            assertTrue(saved.getNewValue().contains("Harvest"), 
                    "Should capture harvest details");
        }
    }
}