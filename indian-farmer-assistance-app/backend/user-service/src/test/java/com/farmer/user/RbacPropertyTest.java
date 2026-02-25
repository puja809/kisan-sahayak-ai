package com.farmer.user;

import com.farmer.user.config.SecurityConfig;
import com.farmer.user.entity.User;
import com.farmer.user.security.CustomAccessDeniedHandler;
import com.farmer.user.security.JwtAuthenticationFilter;
import com.farmer.user.security.JwtService;
import com.farmer.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Property-based tests for Role-Based Access Control (RBAC).
 * 
 * Property 41: Role-Based Access Control
 * Validates: Requirements 22.5
 * 
 * These tests verify the following properties:
 * 1. For any authenticated user with role FARMER, access to admin-only endpoints should be denied (403 Forbidden)
 * 2. For any authenticated user with role ADMIN, access to all endpoints should be allowed (200 OK or appropriate response)
 * 3. For any unauthenticated user, access to protected endpoints should be denied (401 Unauthorized)
 */
@WebMvcTest(controllers = TestController.class)
@Import({SecurityConfig.class, CustomAccessDeniedHandler.class, ObjectMapper.class})
class RbacPropertyTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Nested
    @DisplayName("Property 41.1: Farmer access to admin endpoints should be denied")
    class FarmerAdminEndpointAccess {

        @ParameterizedTest
        @EnumSource(value = User.Role.class, names = {"FARMER"})
        @DisplayName("Farmer should receive 403 Forbidden when accessing admin endpoints")
        void farmerShouldBeDeniedAccessToAdminEndpoints(User.Role role) throws Exception {
            // Property: For any authenticated user with role FARMER, 
            // access to admin-only endpoints should be denied (403 Forbidden)
            
            mockMvc.perform(get("/api/v1/admin/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(user("farmer-user").roles("FARMER")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.error").value("Forbidden"));
        }

        @Test
        @DisplayName("Farmer should be denied access to role management endpoints")
        void farmerShouldBeDeniedAccessToRoleManagement() throws Exception {
            // Property: For any authenticated user with role FARMER, 
            // access to admin-only endpoints should be denied (403 Forbidden)
            
            mockMvc.perform(get("/api/v1/admin/roles")
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(user("farmer-user").roles("FARMER")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.error").value("Forbidden"));
        }
    }

    @Nested
    @DisplayName("Property 41.2: Admin access to all endpoints should be allowed")
    class AdminEndpointAccess {

        @ParameterizedTest
        @EnumSource(value = User.Role.class, names = {"ADMIN"})
        @DisplayName("Admin should receive 200 OK when accessing admin endpoints")
        void adminShouldBeAllowedAccessToAdminEndpoints(User.Role role) throws Exception {
            // Property: For any authenticated user with role ADMIN, 
            // access to all endpoints should be allowed (200 OK or appropriate response)
            
            mockMvc.perform(get("/api/v1/admin/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(user("admin-user").roles("ADMIN")))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Admin should be allowed access to role management endpoints")
        void adminShouldBeAllowedAccessToRoleManagement() throws Exception {
            // Property: For any authenticated user with role ADMIN, 
            // access to all endpoints should be allowed (200 OK or appropriate response)
            
            mockMvc.perform(get("/api/v1/admin/roles")
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(user("admin-user").roles("ADMIN")))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Property 41.3: Unauthenticated access should be denied")
    class UnauthenticatedAccess {

        @Test
        @DisplayName("Unauthenticated user should receive 401 Unauthorized for protected endpoints")
        void unauthenticatedUserShouldBeDeniedAccess() throws Exception {
            // Property: For any unauthenticated user, 
            // access to protected endpoints should be denied (401 Unauthorized)
            
            mockMvc.perform(get("/api/v1/users/profile")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Unauthenticated user should be denied access to admin endpoints")
        void unauthenticatedUserShouldBeDeniedAdminAccess() throws Exception {
            // Property: For any unauthenticated user, 
            // access to protected endpoints should be denied (401 Unauthorized)
            
            mockMvc.perform(get("/api/v1/admin/users")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Property 41.4: Public endpoints should be accessible without authentication")
    class PublicEndpointAccess {

        @Test
        @DisplayName("Public endpoints should be accessible without authentication")
        void publicEndpointsShouldBeAccessible() throws Exception {
            // Verify that public endpoints are accessible without authentication
            
            mockMvc.perform(get("/actuator/health")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }
}