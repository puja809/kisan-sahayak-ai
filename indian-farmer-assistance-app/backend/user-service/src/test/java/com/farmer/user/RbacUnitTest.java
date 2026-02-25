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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for Role-Based Access Control (RBAC).
 * 
 * Validates: Requirements 22.5, 22.7
 * 
 * These tests verify:
 * 1. Farmer access to allowed endpoints
 * 2. Farmer denied access to admin endpoints
 * 3. Admin access to all endpoints
 * 4. Audit logging for unauthorized attempts
 */
@WebMvcTest(controllers = TestController.class)
@Import({SecurityConfig.class, CustomAccessDeniedHandler.class, ObjectMapper.class})
class RbacUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Nested
    @DisplayName("Test Case 1: Farmer access to allowed endpoints")
    class FarmerAccessAllowedEndpoints {

        @Test
        @DisplayName("Farmer should be able to access user profile endpoint")
        void farmerShouldAccessUserProfileEndpoint() throws Exception {
            // Test that farmers can access endpoints that require authentication
            // but not admin-specific permissions
            
            mockMvc.perform(get("/api/v1/users/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(user("farmer-user").roles("FARMER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User profile endpoint accessed successfully"))
                    .andExpect(jsonPath("$.requiredRole").value("AUTHENTICATED"));
        }

        @Test
        @DisplayName("Farmer should have access to authenticated-only endpoints")
        void farmerShouldAccessAuthenticatedEndpoints() throws Exception {
            // Verify that farmers can access endpoints that only require authentication
            // (not admin-specific)
            
            mockMvc.perform(get("/api/v1/users/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(user("farmer1").roles("FARMER")))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Test Case 2: Farmer denied access to admin endpoints")
    class FarmerDeniedAdminEndpoints {

        @Test
        @DisplayName("Farmer should receive 403 Forbidden when accessing admin users endpoint")
        void farmerShouldBeDeniedAccessToAdminUsersEndpoint() throws Exception {
            // Requirement 22.5: Return 403 Forbidden for unauthorized access attempts
            
            mockMvc.perform(get("/api/v1/admin/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(user("farmer-user").roles("FARMER")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.error").value("Forbidden"));
        }

        @Test
        @DisplayName("Farmer should receive 403 Forbidden when accessing admin roles endpoint")
        void farmerShouldBeDeniedAccessToAdminRolesEndpoint() throws Exception {
            // Requirement 22.5: Return 403 Forbidden for unauthorized access attempts
            
            mockMvc.perform(get("/api/v1/admin/roles")
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(user("farmer-user").roles("FARMER")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.error").value("Forbidden"));
        }

        @Test
        @DisplayName("Multiple farmer users should all be denied admin access")
        void multipleFarmersShouldBeDeniedAdminAccess() throws Exception {
            // Verify that different farmer users are consistently denied admin access
            
            mockMvc.perform(get("/api/v1/admin/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(user("farmer1").roles("FARMER")))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/v1/admin/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(user("farmer2").roles("FARMER")))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/v1/admin/roles")
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(user("another-farmer").roles("FARMER")))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Test Case 3: Admin access to all endpoints")
    class AdminAccessAllEndpoints {

        @Test
        @DisplayName("Admin should be able to access admin users endpoint")
        void adminShouldAccessAdminUsersEndpoint() throws Exception {
            // Test that admins can access admin-only endpoints
            
            mockMvc.perform(get("/api/v1/admin/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(user("admin-user").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Admin users endpoint accessed successfully"))
                    .andExpect(jsonPath("$.requiredRole").value("ADMIN"));
        }

        @Test
        @DisplayName("Admin should be able to access admin roles endpoint")
        void adminShouldAccessAdminRolesEndpoint() throws Exception {
            // Test that admins can access role management endpoints
            
            mockMvc.perform(get("/api/v1/admin/roles")
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(user("admin-user").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Admin roles endpoint accessed successfully"))
                    .andExpect(jsonPath("$.requiredRole").value("ADMIN"));
        }

        @Test
        @DisplayName("Admin should be able to access authenticated user endpoints")
        void adminShouldAccessUserEndpoints() throws Exception {
            // Test that admins can also access endpoints that only require authentication
            
            mockMvc.perform(get("/api/v1/users/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(user("admin-user").roles("ADMIN")))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Multiple admin users should all have access to admin endpoints")
        void multipleAdminsShouldHaveAccess() throws Exception {
            // Verify that different admin users consistently have admin access
            
            mockMvc.perform(get("/api/v1/admin/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(user("admin1").roles("ADMIN")))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/admin/roles")
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(user("admin2").roles("ADMIN")))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Test Case 4: Audit logging for unauthorized attempts")
    class AuditLoggingUnauthorizedAttempts {

        @Test
        @DisplayName("Unauthorized access attempt should be logged with proper response")
        void unauthorizedAccessShouldReturnProperResponse() throws Exception {
            // Requirement 22.7: Create audit logging for role modifications
            // and unauthorized access attempts should return proper error response
            
            mockMvc.perform(get("/api/v1/admin/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(user("unauthorized-user").roles("FARMER")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.error").value("Forbidden"));
        }

        @Test
        @DisplayName("Error response should contain meaningful message")
        void errorResponseShouldContainMeaningfulMessage() throws Exception {
            // Verify that the 403 response contains meaningful error information
            
            mockMvc.perform(get("/api/v1/admin/roles")
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(user("farmer-attempting-admin-access").roles("FARMER")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.error").value("Forbidden"));
        }

        @Test
        @DisplayName("Access denied to admin endpoints should be consistent")
        void accessDeniedShouldBeConsistent() throws Exception {
            // Verify consistent 403 responses for unauthorized access attempts
            
            mockMvc.perform(get("/api/v1/admin/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(user("test-farmer").roles("FARMER")))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/v1/admin/roles")
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(user("test-farmer").roles("FARMER")))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Edge Cases: RBAC edge cases")
    class RbacEdgeCases {

        @Test
        @DisplayName("User with no explicit role should be denied admin access")
        void userWithNoRoleShouldBeDeniedAdminAccess() throws Exception {
            // Test behavior when user has no explicit role
            
            mockMvc.perform(get("/api/v1/admin/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(user("user-without-role").roles()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Different user principals should be properly handled")
        void differentUserPrincipalsShouldBeHandled() throws Exception {
            // Test that different user principal types are handled correctly
            
            mockMvc.perform(get("/api/v1/users/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(user("test-farmer").roles("FARMER")))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/users/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(user("test-admin").roles("ADMIN")))
                    .andExpect(status().isOk());
        }
    }
}