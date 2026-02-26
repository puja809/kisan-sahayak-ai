package com.farmer.user;

import com.farmer.user.dto.*;
import com.farmer.user.entity.User;
import com.farmer.user.repository.UserRepository;
import com.farmer.user.security.JwtService;
import com.farmer.user.service.AgriStackService;
import com.farmer.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 * Requirements: 11.1, 11.2, 11.6, 11.7, 11.8
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AgriStackService agristackService;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .farmerId("FARMER-12345678")
                .aadhaarHash("aadhaarhash123")
                .name("Test Farmer")
                .phone("9876543210")
                .email("test@farmer.com")
                .preferredLanguage("en")
                .state("Maharashtra")
                .district("Pune")
                .village("Test Village")
                .pinCode("411001")
                .role(User.Role.FARMER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldRegisterNewUser() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .name("New Farmer")
                .phone("9876543211")
                .email("new@farmer.com")
                .state("Maharashtra")
                .district("Pune")
                .village("New Village")
                .aadhaarHash("aadhaarhash456")
                .preferredLanguage("en")
                .build();

        when(userRepository.existsByPhone(request.getPhone())).thenReturn(false);
        when(userRepository.existsByAadhaarHash(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-token");
        when(jwtService.getExpirationSeconds()).thenReturn(86400L);

        // When
        AuthResponse response = userService.register(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertTrue(response.getIsNewUser());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldNotRegisterUserWithExistingPhone() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .name("Test Farmer")
                .phone("9876543210")
                .state("Maharashtra")
                .district("Pune")
                .build();

        when(userRepository.existsByPhone(request.getPhone())).thenReturn(true);

        // When & Then
        assertThrows(UserService.AuthenticationException.class, () -> userService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldLoginWithValidCredentials() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .phone("9876543210")
                .otp("123456")
                .build();

        when(userRepository.findByPhone(request.getPhone())).thenReturn(Optional.of(testUser));
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-token");
        when(jwtService.getExpirationSeconds()).thenReturn(86400L);

        // When
        AuthResponse response = userService.login(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertFalse(response.getIsNewUser());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldNotLoginWithInvalidPhone() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .phone("9999999999")
                .otp("123456")
                .build();

        when(userRepository.findByPhone(request.getPhone())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserService.AuthenticationException.class, () -> userService.login(request));
    }

    @Test
    void shouldNotLoginWithInactiveAccount() {
        // Given
        testUser.setIsActive(false);
        LoginRequest request = LoginRequest.builder()
                .phone("9876543210")
                .otp("123456")
                .build();

        when(userRepository.findByPhone(request.getPhone())).thenReturn(Optional.of(testUser));

        // When & Then
        assertThrows(UserService.AuthenticationException.class, () -> userService.login(request));
    }

    @Test
    void shouldNotLoginWithInvalidOtp() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .phone("9876543210")
                .otp("wrong-otp")
                .build();

        when(userRepository.findByPhone(request.getPhone())).thenReturn(Optional.of(testUser));

        // When & Then
        assertThrows(UserService.AuthenticationException.class, () -> userService.login(request));
    }

    @Test
    void shouldRefreshToken() {
        // Given
        String refreshToken = "valid-refresh-token";
        when(jwtService.validateToken(refreshToken)).thenReturn(true);
        when(jwtService.extractUsername(refreshToken)).thenReturn(testUser.getFarmerId());
        when(userRepository.findByFarmerId(testUser.getFarmerId())).thenReturn(Optional.of(testUser));
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("new-refresh-token");
        when(jwtService.getExpirationSeconds()).thenReturn(86400L);

        // When
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken(refreshToken)
                .build();
        AuthResponse response = userService.refreshToken(refreshToken);

        // Then
        assertNotNull(response);
        assertNotNull(response.getAccessToken());
    }

    @Test
    void shouldNotRefreshInvalidToken() {
        // Given
        String invalidToken = "invalid-refresh-token";
        when(jwtService.validateToken(invalidToken)).thenReturn(false);

        // When & Then
        assertThrows(UserService.AuthenticationException.class, () -> userService.refreshToken(invalidToken));
    }

    @Test
    void shouldGetProfile() {
        // Given
        when(userRepository.findByFarmerId(testUser.getFarmerId())).thenReturn(Optional.of(testUser));

        // When
        UserResponse response = userService.getProfile(testUser.getFarmerId());

        // Then
        assertNotNull(response);
        assertEquals(testUser.getFarmerId(), response.getFarmerId());
        assertEquals(testUser.getName(), response.getName());
        assertEquals(testUser.getPhone(), response.getPhone());
    }

    @Test
    void shouldNotGetProfileForNonExistentUser() {
        // Given
        when(userRepository.findByFarmerId("NON-EXISTENT")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserService.UserNotFoundException.class, () -> userService.getProfile("NON-EXISTENT"));
    }

    @Test
    void shouldUpdateProfile() {
        // Given
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .name("Updated Name")
                .email("updated@farmer.com")
                .village("Updated Village")
                .build();

        when(userRepository.findByFarmerId(testUser.getFarmerId())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UserResponse response = userService.updateProfile(testUser.getFarmerId(), request);

        // Then
        assertNotNull(response);
        assertEquals("Updated Name", response.getName());
        assertEquals("updated@farmer.com", response.getEmail());
        assertEquals("Updated Village", response.getVillage());
    }

    @Test
    void shouldDeactivateAccount() {
        // Given
        when(userRepository.findByFarmerId(testUser.getFarmerId())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        userService.deactivateAccount(testUser.getFarmerId());

        // Then
        verify(userRepository).save(argThat(user -> !user.getIsActive()));
    }

    @Test
    void shouldLogout() {
        // When
        userService.logout(testUser.getFarmerId());

        // Then - no exception means success
        // In stateless JWT, logout is handled client-side
    }
}