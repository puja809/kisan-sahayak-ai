package com.farmer.user;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Test controller for RBAC property testing.
 * Provides endpoints with different access levels to test role-based access control.
 */
@RestController
@RequestMapping("/api/v1")
public class TestController {

    /**
     * Admin-only endpoint for testing RBAC.
     * Requires ADMIN role.
     */
    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> adminUsersEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Admin users endpoint accessed successfully");
        response.put("requiredRole", "ADMIN");
        return ResponseEntity.ok(response);
    }

    /**
     * Admin-only endpoint for role management testing.
     * Requires ADMIN role.
     */
    @GetMapping("/admin/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> adminRolesEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Admin roles endpoint accessed successfully");
        response.put("requiredRole", "ADMIN");
        return ResponseEntity.ok(response);
    }

    /**
     * Protected user endpoint for testing authenticated access.
     * Requires authentication (any role).
     */
    @GetMapping("/users/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> userProfileEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User profile endpoint accessed successfully");
        response.put("requiredRole", "AUTHENTICATED");
        return ResponseEntity.ok(response);
    }
}